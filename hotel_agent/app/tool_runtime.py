# -*- coding: utf-8 -*-
from __future__ import annotations

from datetime import date
from decimal import Decimal
from typing import Any, Callable

from .backend_client import BackendClientError, get_backend_client
from .tool_schemas import TOOL_SCHEMAS


def _normalize_value(value: Any) -> Any:
    if isinstance(value, Decimal):
        return float(value)
    if isinstance(value, date):
        return value.isoformat()
    if isinstance(value, list):
        return [_normalize_value(item) for item in value]
    if isinstance(value, dict):
        return {key: _normalize_value(item) for key, item in value.items()}
    return value


def _compact_payload(source: Any) -> Any:
    if isinstance(source, dict):
        compacted = {key: _compact_payload(value) for key, value in source.items()}
        return {key: value for key, value in compacted.items() if value not in (None, '', [], {})}
    if isinstance(source, list):
        compacted_list = [_compact_payload(item) for item in source]
        return [item for item in compacted_list if item not in (None, '', [], {})]
    return source


def _success(tool_name: str, summary: str, **payload: Any) -> dict[str, Any]:
    return {
        'ok': True,
        'tool': tool_name,
        'summary': summary,
        **_normalize_value(payload),
    }


def _error(
    tool_name: str,
    message: str,
    *,
    status_code: int | None = None,
    business_code: int | None = None,
) -> dict[str, Any]:
    return {
        'ok': False,
        'tool': tool_name,
        'error': {
            'message': message,
            'status_code': status_code,
            'business_code': business_code,
        },
    }


def _missing_guest_id(tool_name: str) -> dict[str, Any]:
    return _error(tool_name, 'GUEST_ID_MISSING')


def _normalize_order_query_filters(filters: dict[str, Any] | None, guest_id: int | None = None) -> dict[str, Any]:
    payload = _compact_payload(filters or {})
    if guest_id is not None and payload.get('guestId') is None:
        payload['guestId'] = guest_id
    return payload


def _build_full_address(order: dict[str, Any]) -> str:
    if order.get('hotelFullAddress'):
        return str(order['hotelFullAddress'])
    parts = [order.get('province'), order.get('city'), order.get('district'), order.get('address') or order.get('hotelAddress')]
    return ''.join(str(part) for part in parts if part)


def _normalize_order_record(order: dict[str, Any] | None) -> dict[str, Any]:
    if not isinstance(order, dict):
        return {}

    order_id = order.get('orderId') or order.get('reservationId') or order.get('id')
    normalized = dict(order)
    normalized['orderId'] = order_id
    normalized['reservationId'] = order.get('reservationId') or order_id
    normalized['orderNo'] = order.get('orderNo') or (str(order_id) if order_id is not None else '')
    normalized['roomTypeName'] = order.get('roomTypeName') or order.get('typeName') or ''
    normalized['hotelAddress'] = order.get('hotelAddress') or order.get('address') or _build_full_address(order)
    normalized['hotelFullAddress'] = _build_full_address(order)
    normalized['statusDescription'] = order.get('statusDescription') or order.get('statusText') or order.get('status') or ''
    return _normalize_value(normalized)


def _normalize_order_records(orders: list[dict[str, Any]] | None) -> list[dict[str, Any]]:
    return [_normalize_order_record(order) for order in (orders or []) if isinstance(order, dict)]


def search_hotels(
    location: dict[str, Any] | None = None,
    facilities: dict[str, Any] | None = None,
    roomType: dict[str, Any] | None = None,
    hotelName: str | None = None,
    minPrice: float | None = None,
    maxPrice: float | None = None,
    rating: float | None = None,
    checkInDate: str | None = None,
    checkOutDate: str | None = None,
    roomCount: int | None = None,
) -> dict[str, Any]:
    payload = _compact_payload(
        {
            'location': location or {},
            'facilities': facilities or {},
            'roomType': roomType or {},
            'hotelName': hotelName,
            'minPrice': minPrice,
            'maxPrice': maxPrice,
            'rating': rating,
            'checkInDate': checkInDate,
            'checkOutDate': checkOutDate,
            'roomCount': roomCount,
        }
    )

    try:
        hotels = get_backend_client().request('POST', '/agent/hotels/search', json=payload) or []
        return _success(
            'search_hotels',
            f'已找到 {len(hotels)} 家酒店',
            query=payload,
            count=len(hotels),
            hotels=hotels,
        )
    except BackendClientError as exc:
        return _error('search_hotels', str(exc), status_code=exc.status_code, business_code=exc.business_code)


def search_orders(filters: dict[str, Any] | None = None, guest_id: int | None = None) -> dict[str, Any]:
    payload = _normalize_order_query_filters(filters, guest_id)
    if payload.get('guestId') is None:
        return _missing_guest_id('search_orders')

    try:
        orders = get_backend_client().request('POST', '/agent/orders/search', json=payload) or []
        normalized_orders = _normalize_order_records(orders)
        return _success(
            'search_orders',
            f'已找到 {len(normalized_orders)} 条订单',
            query=payload,
            total=len(normalized_orders),
            orders=normalized_orders,
        )
    except BackendClientError as exc:
        return _error('search_orders', str(exc), status_code=exc.status_code, business_code=exc.business_code)


def get_recent_orders(guest_id: int, limit: int | None = None) -> dict[str, Any]:
    params = _compact_payload({'guestId': guest_id, 'limit': limit})
    try:
        orders = get_backend_client().request('GET', '/agent/orders/recent', params=params) or []
        normalized_orders = _normalize_order_records(orders)
        return _success(
            'get_recent_orders',
            f'已找到 {len(normalized_orders)} 条最近订单',
            query=params,
            guest_id=guest_id,
            count=len(normalized_orders),
            orders=normalized_orders,
        )
    except BackendClientError as exc:
        return _error('get_recent_orders', str(exc), status_code=exc.status_code, business_code=exc.business_code)


def get_order_detail(reservation_id: int, guest_id: int) -> dict[str, Any]:
    try:
        detail = get_backend_client().request('GET', f'/agent/orders/{reservation_id}', params={'guestId': guest_id})
        return _success('get_order_detail', '已查询到订单详情', reservation_id=reservation_id, order=_normalize_order_record(detail))
    except BackendClientError as exc:
        return _error('get_order_detail', str(exc), status_code=exc.status_code, business_code=exc.business_code)


def update_order(
    reservation_id: int,
    guest_id: int,
    check_in_date: str | None = None,
    check_out_date: str | None = None,
    room_type_id: int | None = None,
    room_type_keyword: str | None = None,
) -> dict[str, Any]:
    payload = _compact_payload(
        {
            'guestId': guest_id,
            'checkInDate': check_in_date,
            'checkOutDate': check_out_date,
            'roomTypeId': room_type_id,
            'roomTypeKeyword': room_type_keyword,
        }
    )
    try:
        detail = get_backend_client().request('PUT', f'/agent/orders/{reservation_id}', json=payload)
        return _success('update_order', '已完成订单修改', reservation_id=reservation_id, updated_fields=payload, order=_normalize_order_record(detail))
    except BackendClientError as exc:
        return _error('update_order', str(exc), status_code=exc.status_code, business_code=exc.business_code)


def cancel_order(reservation_id: int, guest_id: int, reason: str | None = None) -> dict[str, Any]:
    payload = _compact_payload({'guestId': guest_id, 'reason': reason})
    try:
        detail = get_backend_client().request('POST', f'/agent/orders/{reservation_id}/cancel', json=payload)
        return _success('cancel_order', '已完成订单取消', reservation_id=reservation_id, reason=reason, order=_normalize_order_record(detail))
    except BackendClientError as exc:
        return _error('cancel_order', str(exc), status_code=exc.status_code, business_code=exc.business_code)


def update_order_by_query(
    filters: dict[str, Any] | None = None,
    updates: dict[str, Any] | None = None,
    guest_id: int | None = None,
) -> dict[str, Any]:
    query = _normalize_order_query_filters(filters, guest_id)
    if query.get('guestId') is None:
        return _missing_guest_id('update_order_by_query')

    search_result = search_orders(query)
    if not search_result.get('ok'):
        return {
            **search_result,
            'tool': 'update_order_by_query',
        }

    orders = search_result.get('orders') or []
    if not orders:
        return _success(
            'update_order_by_query',
            '没有找到符合条件的订单',
            query=query,
            matched=False,
            multiple=False,
            candidates=[],
            total=0,
            order={},
        )
    if len(orders) > 1:
        return _success(
            'update_order_by_query',
            '匹配到多条订单，需要先确认目标订单',
            query=query,
            matched=False,
            multiple=True,
            candidates=orders,
            total=len(orders),
            order={},
        )

    target = orders[0]
    normalized_updates = _compact_payload(updates or {})
    result = update_order(
        reservation_id=target.get('reservationId') or target.get('orderId'),
        guest_id=query['guestId'],
        check_in_date=normalized_updates.get('checkInDate'),
        check_out_date=normalized_updates.get('checkOutDate'),
        room_type_id=normalized_updates.get('roomTypeId'),
        room_type_keyword=normalized_updates.get('roomTypeKeyword'),
    )
    if not result.get('ok'):
        return {
            **result,
            'tool': 'update_order_by_query',
            'query': query,
            'matched': True,
            'multiple': False,
            'candidates': [],
            'total': 1,
            'order': result.get('order') or {},
        }

    return _success(
        'update_order_by_query',
        '已完成订单修改',
        query=query,
        matched=True,
        multiple=False,
        candidates=[],
        total=1,
        order=result.get('order') or {},
    )


def cancel_order_by_query(
    filters: dict[str, Any] | None = None,
    reason: str | None = None,
    guest_id: int | None = None,
) -> dict[str, Any]:
    query = _normalize_order_query_filters(filters, guest_id)
    if query.get('guestId') is None:
        return _missing_guest_id('cancel_order_by_query')

    search_result = search_orders(query)
    if not search_result.get('ok'):
        return {
            **search_result,
            'tool': 'cancel_order_by_query',
        }

    orders = search_result.get('orders') or []
    if not orders:
        return _success(
            'cancel_order_by_query',
            '没有找到符合条件的订单',
            query=query,
            matched=False,
            multiple=False,
            candidates=[],
            total=0,
            order={},
        )
    if len(orders) > 1:
        return _success(
            'cancel_order_by_query',
            '匹配到多条订单，需要先确认目标订单',
            query=query,
            matched=False,
            multiple=True,
            candidates=orders,
            total=len(orders),
            order={},
        )

    target = orders[0]
    result = cancel_order(
        reservation_id=target.get('reservationId') or target.get('orderId'),
        guest_id=query['guestId'],
        reason=reason,
    )
    if not result.get('ok'):
        return {
            **result,
            'tool': 'cancel_order_by_query',
            'query': query,
            'matched': True,
            'multiple': False,
            'candidates': [],
            'total': 1,
            'order': result.get('order') or {},
        }

    return _success(
        'cancel_order_by_query',
        '已完成订单取消',
        query=query,
        matched=True,
        multiple=False,
        candidates=[],
        total=1,
        order=result.get('order') or {},
    )


TOOL_FUNC_MAP: dict[str, Callable[..., dict[str, Any]]] = {
    'search_hotels': search_hotels,
    'search_orders': search_orders,
    'get_recent_orders': get_recent_orders,
    'get_order_detail': get_order_detail,
    'update_order': update_order,
    'update_order_by_query': update_order_by_query,
    'cancel_order': cancel_order,
    'cancel_order_by_query': cancel_order_by_query,
}

TOOLS = TOOL_SCHEMAS
