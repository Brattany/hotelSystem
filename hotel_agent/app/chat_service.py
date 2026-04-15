# -*- coding: utf-8 -*-
from __future__ import annotations

import json
import re
from copy import deepcopy
from typing import Any

from .config import get_settings
from .llm_client import get_llm_client
from .prompts import SYSTEM_PROMPT
from .schemas import ChatRequest, ChatResponse, ToolExecutionRecord
from .tools import TOOL_FUNC_MAP, TOOLS

HOTEL_INTENT = "search_hotels"
ORDER_QUERY_INTENT = "query_orders"
ORDER_UPDATE_INTENT = "update_order"
ORDER_CANCEL_INTENT = "cancel_order"
GENERAL_INTENT = "general"

HOTEL_CHAIN_KEYWORDS = ("如家", "汉庭", "全季", "亚朵", "维也纳", "锦江之星", "7天", "格林豪泰")
HOTEL_SEARCH_HINTS = ("酒店", "宾馆", "民宿", "客栈", "住宿", "旅馆", "住哪")
HOTEL_SUFFIXES = ("酒店", "宾馆", "民宿", "客栈", "公寓", "度假村")
LANDMARK_SUFFIXES = ("路", "街", "道", "巷", "里", "镇", "广场", "商圈", "地铁站", "车站", "机场", "公园", "景区", "大学")
DISTRICT_SUFFIXES = ("区", "县", "市")
CITY_SUFFIXES = ("市", "州", "地区", "盟")
ROOM_TYPE_SUFFIX = "房"
MATCH_LEVEL_EXACT = "exact"
MATCH_LEVEL_FUZZY = "fuzzy"
MATCH_LEVEL_CITY_FALLBACK = "city_fallback"
DEFAULT_FACILITY_MODE = "all"
ACTIVE_ORDER_STATUS = "booked"

FACILITY_KEYWORDS = {
    "wifi": ("wifi", "wi-fi", "无线网", "无线网络"),
    "breakfast": ("breakfast", "早餐", "含早", "带早餐", "提供早餐"),
    "parking": ("parking", "停车场", "可停车", "停车位", "有停车场"),
}
ORDER_QUERY_HINTS = ("订单", "预订", "入住时间", "退房时间", "离店时间", "房型", "取消", "改期", "修改")
ORDER_CANCEL_HINTS = ("取消", "撤销", "不要了")
ORDER_UPDATE_HINTS = ("修改", "更改", "改为", "改成", "换为", "换成", "调整为", "变更为", "入住时间", "退房时间", "房型")


class ChatService:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.client = get_llm_client()

    def chat(self, request: ChatRequest) -> ChatResponse:
        direct_hotel_response = self._try_handle_hotel_search(request)
        if direct_hotel_response is not None:
            return direct_hotel_response

        direct_order_response = self._try_handle_order_request(request)
        if direct_order_response is not None:
            return direct_order_response

        messages: list[dict[str, Any]] = [{"role": "system", "content": SYSTEM_PROMPT}]
        messages.extend(message.model_dump() for message in request.history)
        messages.append({"role": "user", "content": request.message})
        used_tools: list[ToolExecutionRecord] = []
        tool_results: list[dict[str, Any]] = []

        for _ in range(self.settings.llm_max_tool_rounds):
            response = self.client.chat.completions.create(
                model=self.settings.llm_model,
                messages=messages,
                tools=TOOLS,
                tool_choice="auto",
            )
            assistant_message = response.choices[0].message
            tool_calls = getattr(assistant_message, "tool_calls", None) or []
            if not tool_calls:
                structured_data = self._build_structured_data(tool_results)
                intent = self._infer_intent(used_tools, request.message)
                tool_error = self._extract_tool_error(tool_results)
                reply = self._build_reply(intent, structured_data, assistant_message.content or "", tool_error)
                return ChatResponse(intent=intent, structured_data=structured_data, reply=reply, success=tool_error is None, error=tool_error, used_tools=used_tools)

            messages.append({"role": "assistant", "content": assistant_message.content or "", "tool_calls": [tool_call.model_dump() for tool_call in tool_calls]})
            for tool_call in tool_calls:
                tool_name = tool_call.function.name
                tool_args = json.loads(tool_call.function.arguments or "{}")
                tool_args = self._inject_guest_id(tool_name, tool_args, request.guest_id)
                tool_result = self._execute_tool(tool_name, tool_args)
                tool_results.append(tool_result)
                used_tools.append(ToolExecutionRecord(tool_name=tool_name, tool_args=tool_args, tool_result=tool_result))
                messages.append({"role": "tool", "tool_call_id": tool_call.id, "content": json.dumps(tool_result, ensure_ascii=False)})

        raise RuntimeError("Tool calling stopped because the maximum round limit was reached.")

    def _try_handle_hotel_search(self, request: ChatRequest) -> ChatResponse | None:
        if self._looks_like_order_request(request.message):
            return None
        query = self._extract_hotel_query(request.message)
        if self._has_valid_hotel_condition(query):
            tool_results = self._search_hotels_with_fallback(query)
            used_tools = [ToolExecutionRecord(tool_name=HOTEL_INTENT, tool_args=result.get("raw_tool_args") or query, tool_result=result) for result in tool_results]
            selected_result = tool_results[-1] if tool_results else {}
            structured_data = self._build_structured_data(tool_results, preferred_query=query)
            tool_error = self._extract_tool_error(tool_results)
            return ChatResponse(intent=HOTEL_INTENT, structured_data=structured_data, reply=self._build_hotel_reply(structured_data, tool_error, selected_result.get("matchLevel")), success=tool_error is None, error=tool_error, used_tools=used_tools)
        if self._looks_like_hotel_search(request.message):
            return ChatResponse(intent=HOTEL_INTENT, structured_data=self._empty_hotel_structured_data(), reply="请告诉我至少一个检索条件，例如城市、区县、地标、酒店名、服务条件或房型，我就可以直接为您查询。", success=True, error=None, used_tools=[])
        return None

    def _try_handle_order_request(self, request: ChatRequest) -> ChatResponse | None:
        action = self._detect_order_action(request.message)
        if action is None:
            return None
        if request.guest_id is None:
            return ChatResponse(intent=action, structured_data={}, reply="未能识别当前登录用户，请先登录后再查询订单。", success=False, error="GUEST_ID_MISSING", used_tools=[])

        order_query = self._extract_order_query(request.message, request.guest_id, action, request.order_candidates)
        if action == ORDER_QUERY_INTENT:
            tool_args = {"filters": order_query, "guest_id": request.guest_id}
            tool_result = self._execute_tool("search_orders", tool_args)
        elif action == ORDER_UPDATE_INTENT:
            order_updates = self._extract_order_updates(request.message)
            if not order_updates:
                return ChatResponse(intent=ORDER_UPDATE_INTENT, structured_data={"query": order_query, "matched": False, "multiple": False, "candidates": [], "order": {}}, reply="请告诉我想修改的内容，例如新的入住时间、退房时间或房型。", success=True, error=None, used_tools=[])
            tool_args = {"filters": order_query, "updates": order_updates, "guest_id": request.guest_id}
            tool_result = self._execute_tool("update_order_by_query", tool_args)
        else:
            tool_args = {"filters": order_query, "reason": self._extract_cancel_reason(request.message), "guest_id": request.guest_id}
            tool_result = self._execute_tool("cancel_order_by_query", tool_args)

        structured_data = self._build_structured_data([tool_result])
        tool_error = self._extract_tool_error([tool_result])
        return ChatResponse(intent=action, structured_data=structured_data, reply=self._build_reply(action, structured_data, "", tool_error), success=tool_error is None, error=tool_error, used_tools=[ToolExecutionRecord(tool_name=tool_result.get("tool") or action, tool_args=tool_args, tool_result=tool_result)])
    def _search_hotels_with_fallback(self, query: dict[str, Any]) -> list[dict[str, Any]]:
        attempts = self._build_hotel_search_attempts(query)
        tool_results: list[dict[str, Any]] = []
        for attempt in attempts:
            tool_result = self._execute_tool(HOTEL_INTENT, attempt["tool_args"])
            tool_result["raw_tool_args"] = deepcopy(attempt["tool_args"])
            tool_result["query"] = self._compact_query_object(attempt["response_query"])
            tool_result["matchLevel"] = attempt["matchLevel"]
            tool_results.append(tool_result)
            if not tool_result.get("ok"):
                break
            if tool_result.get("count", 0) > 0:
                return tool_results
        return tool_results or [{"ok": True, "tool": HOTEL_INTENT, "summary": "", "count": 0, "hotels": [], "query": self._compact_query_object(query), "raw_tool_args": deepcopy(query), "matchLevel": MATCH_LEVEL_EXACT}]

    def _build_hotel_search_attempts(self, query: dict[str, Any]) -> list[dict[str, Any]]:
        exact_query = deepcopy(query)
        exact_location = exact_query.get("location") or {}
        if exact_location:
            exact_location.pop("districtKeyword", None)
            if not exact_location.get("district") and query.get("location", {}).get("districtKeyword"):
                exact_location["district"] = query["location"]["districtKeyword"]
            exact_query["location"] = self._compact_query_object(exact_location)

        fuzzy_query = deepcopy(query)
        fuzzy_location = fuzzy_query.get("location") or {}
        if fuzzy_location.get("district") and not fuzzy_location.get("districtKeyword"):
            fuzzy_location["districtKeyword"] = fuzzy_location["district"]
        fuzzy_query["location"] = self._compact_query_object(fuzzy_location)

        city_fallback_query = self._build_city_fallback_query(query)
        attempts: list[dict[str, Any]] = []
        if self._has_valid_hotel_condition(exact_query):
            attempts.append({"matchLevel": MATCH_LEVEL_EXACT, "tool_args": exact_query, "response_query": exact_query})
        if self._should_add_fuzzy_attempt(query, exact_query, fuzzy_query):
            attempts.append({"matchLevel": MATCH_LEVEL_FUZZY, "tool_args": fuzzy_query, "response_query": fuzzy_query})
        if city_fallback_query is not None and city_fallback_query != exact_query and city_fallback_query != fuzzy_query:
            attempts.append({"matchLevel": MATCH_LEVEL_CITY_FALLBACK, "tool_args": city_fallback_query, "response_query": city_fallback_query})
        return attempts

    @staticmethod
    def _should_add_fuzzy_attempt(original_query: dict[str, Any], exact_query: dict[str, Any], fuzzy_query: dict[str, Any]) -> bool:
        if fuzzy_query == exact_query:
            return False
        location = original_query.get("location") or {}
        return any(location.get(key) for key in ("districtKeyword", "district", "street", "addressKeyword")) or bool(original_query.get("hotelName"))

    def _build_city_fallback_query(self, query: dict[str, Any]) -> dict[str, Any] | None:
        location = query.get("location") or {}
        city = location.get("city")
        if not city:
            return None
        fallback_query = deepcopy(query)
        fallback_location = {"city": city}
        if location.get("province"):
            fallback_location["province"] = location["province"]
        fallback_query["location"] = fallback_location
        return self._compact_query_object(fallback_query)

    def _execute_tool(self, tool_name: str, tool_args: dict[str, Any]) -> dict[str, Any]:
        tool_func = TOOL_FUNC_MAP.get(tool_name)
        if tool_func is None:
            return {"ok": False, "tool": tool_name, "error": {"message": f"Tool not found: {tool_name}"}}
        try:
            return tool_func(**tool_args)
        except Exception as exc:  # noqa: BLE001
            return {"ok": False, "tool": tool_name, "error": {"message": str(exc)}}

    @staticmethod
    def _inject_guest_id(tool_name: str, tool_args: dict[str, Any], guest_id: int | None) -> dict[str, Any]:
        if guest_id is None:
            return tool_args
        if tool_name in {"get_recent_orders", "get_order_detail", "update_order", "cancel_order"} and tool_args.get("guest_id") is None:
            tool_args["guest_id"] = guest_id
        if tool_name in {"search_orders", "update_order_by_query", "cancel_order_by_query"}:
            tool_args.setdefault("guest_id", guest_id)
            filters = tool_args.get("filters") or {}
            if isinstance(filters, dict) and filters.get("guestId") is None:
                filters["guestId"] = guest_id
                tool_args["filters"] = filters
        return tool_args

    def _infer_intent(self, used_tools: list[ToolExecutionRecord], message: str) -> str:
        if used_tools:
            tool_name = used_tools[-1].tool_name
            return {"search_orders": ORDER_QUERY_INTENT, "get_recent_orders": ORDER_QUERY_INTENT, "update_order_by_query": ORDER_UPDATE_INTENT, "cancel_order_by_query": ORDER_CANCEL_INTENT}.get(tool_name, tool_name)
        if self._looks_like_hotel_search(message):
            return HOTEL_INTENT
        order_action = self._detect_order_action(message)
        if order_action is not None:
            return order_action
        return GENERAL_INTENT

    def _build_structured_data(self, tool_results: list[dict[str, Any]], preferred_query: dict[str, Any] | None = None) -> dict[str, Any]:
        if not tool_results:
            return {}
        result = tool_results[-1]
        tool_name = result.get("tool")
        if tool_name == HOTEL_INTENT:
            return {"query": result.get("query") or self._compact_query_object(preferred_query or {}), "total": result.get("count", 0), "hotels": result.get("hotels", []), "match_level": result.get("matchLevel")}
        if tool_name in {"search_orders", "get_recent_orders"}:
            return {"query": result.get("query") or {}, "total": result.get("total", result.get("count", 0)), "orders": result.get("orders", [])}
        if tool_name == "get_order_detail":
            return {"reservation": result.get("order")}
        if tool_name in {"update_order", "update_order_by_query", "cancel_order", "cancel_order_by_query"}:
            return {"query": result.get("query") or {}, "total": result.get("total", len(result.get("candidates", []) or [])), "matched": result.get("matched", result.get("ok", False)), "multiple": result.get("multiple", False), "candidates": result.get("candidates", []), "order": result.get("order") or {}, "reservation": result.get("order") or {}}
        return result

    @staticmethod
    def _extract_tool_error(tool_results: list[dict[str, Any]]) -> str | None:
        for result in reversed(tool_results):
            if not result.get("ok"):
                error = result.get("error") or {}
                return error.get("message") or "Tool execution failed."
        return None

    def _build_reply(self, intent: str, structured_data: dict[str, Any], assistant_content: str, tool_error: str | None) -> str:
        if intent == HOTEL_INTENT:
            return self._build_hotel_reply(structured_data, tool_error, structured_data.get("match_level"))
        if intent == ORDER_QUERY_INTENT:
            return self._build_order_query_reply(structured_data, tool_error)
        if intent in {ORDER_UPDATE_INTENT, ORDER_CANCEL_INTENT}:
            return self._build_order_action_reply(intent, structured_data, tool_error)
        content = (assistant_content or "").strip()
        return content or self._default_reply(intent, structured_data)

    def _build_hotel_reply(self, structured_data: dict[str, Any], tool_error: str | None = None, match_level: str | None = None) -> str:
        if tool_error:
            return f"酒店查询失败：{tool_error}"
        total = structured_data.get("total", 0)
        query = structured_data.get("query") or {}
        location = query.get("location") or {}
        if total <= 0:
            return "暂时没有查到符合条件的酒店。您可以继续补充更具体的地址、服务条件或房型要求，我再帮您缩小范围。"
        if match_level == MATCH_LEVEL_FUZZY:
            return f"没有找到完全精准匹配的结果，先为您展示 {total} 家相关酒店。您可以继续补充更具体的街道、服务或房型要求。"
        if match_level == MATCH_LEVEL_CITY_FALLBACK:
            city = location.get("city") or "当前城市"
            return f"没有找到更精准的区域结果，先为您展示 {city} 范围内的 {total} 家酒店。您可以继续补充区县、服务条件或房型要求。"
        return f"已为您找到 {total} 家符合条件的酒店，列表已经展开。您还可以继续补充地址、酒店服务或房型条件。"

    def _build_order_query_reply(self, structured_data: dict[str, Any], tool_error: str | None = None) -> str:
        if tool_error == "GUEST_ID_MISSING":
            return "未能识别当前登录用户，请先登录后再查询订单。"
        if tool_error:
            return f"订单查询失败：{tool_error}"
        total = structured_data.get("total", 0)
        if total <= 0:
            return "没有找到符合条件的订单。您可以补充酒店名、城市、区县或时间范围后再试。"
        return f"已为您找到 {total} 条订单，候选列表已展示。您可以继续指定其中某一条进行修改或取消。"

    def _build_order_action_reply(self, intent: str, structured_data: dict[str, Any], tool_error: str | None = None) -> str:
        if tool_error == "GUEST_ID_MISSING":
            return "未能识别当前登录用户，请先登录后再查询订单。"
        if tool_error:
            prefix = "订单修改失败" if intent == ORDER_UPDATE_INTENT else "订单取消失败"
            return f"{prefix}：{tool_error}"
        if structured_data.get("multiple"):
            return "为您找到多条相关订单，请先确认具体要处理哪一条。"
        if not structured_data.get("matched"):
            return "没有找到符合条件的订单。您可以补充更具体的酒店名、地点或时间范围。"
        hotel_name = (structured_data.get("order") or {}).get("hotelName") or "目标酒店"
        return f"已为您完成 {hotel_name} 订单的{'修改' if intent == ORDER_UPDATE_INTENT else '取消'}，最新结果已展示。"

    @staticmethod
    def _default_reply(intent: str, structured_data: dict[str, Any]) -> str:
        if intent == ORDER_QUERY_INTENT:
            return f"已为您查询到 {structured_data.get('total', 0)} 条订单。"
        if intent == "get_order_detail":
            return "已为您查询到订单详情。"
        if intent == ORDER_UPDATE_INTENT:
            return "订单修改已处理。"
        if intent == ORDER_CANCEL_INTENT:
            return "订单取消已处理。"
        return "已收到您的问题。"
    def _extract_hotel_query(self, message: str) -> dict[str, Any]:
        content = self._normalize_message(message)
        if not content:
            return {}
        location = self._extract_location_tokens(content)
        facilities = self._extract_facilities(content)
        room_type = self._extract_room_type(content)
        hotel_name = self._extract_hotel_name(content, location)
        min_price, max_price = self._extract_price_range(content)
        rating = self._extract_rating(content)
        query: dict[str, Any] = {}
        if location:
            query["location"] = location
        if facilities:
            query["facilities"] = facilities
        if room_type:
            query["roomType"] = room_type
        if hotel_name:
            query["hotelName"] = hotel_name
        if min_price is not None:
            query["minPrice"] = min_price
        if max_price is not None:
            query["maxPrice"] = max_price
        if rating is not None:
            query["rating"] = rating
        return self._compact_query_object(query)

    def _extract_order_query(self, message: str, guest_id: int, action: str, order_candidates: list[dict[str, Any]] | None = None) -> dict[str, Any]:
        content = self._normalize_message(message)
        location = self._extract_location_tokens(content)
        hotel_name = self._extract_hotel_name(content, location)
        recent_days = self._extract_recent_days(content)
        limit = 1 if any(keyword in content for keyword in ("最新订单", "最近预订", "最近一笔", "最近的订单")) else 10
        status = self._extract_order_status(content)
        if action in {ORDER_UPDATE_INTENT, ORDER_CANCEL_INTENT} and not status:
            status = ACTIVE_ORDER_STATUS
        query = self._compact_query_object({
            "guestId": guest_id,
            "reservationId": self._extract_explicit_order_id(content),
            "recentDays": recent_days,
            "province": location.get("province"),
            "city": location.get("city"),
            "district": location.get("district") or location.get("districtKeyword"),
            "hotelName": hotel_name,
            "roomTypeKeyword": self._extract_source_order_room_type(content),
            "status": status,
            "sort": "createdAt_desc",
            "limit": limit,
        })
        candidate_query = self._resolve_candidate_order_query(content, guest_id, order_candidates or [])
        if candidate_query:
            query.update(candidate_query)
        return self._compact_query_object(query)

    def _extract_order_updates(self, message: str) -> dict[str, Any]:
        content = self._normalize_message(message)
        updates: dict[str, Any] = {}
        check_in_match = re.search(r"(?:入住时间|预计入住时间)(?:改为|改成|改到|调整为|变更为|为)(\d{4}[/-]\d{1,2}[/-]\d{1,2}|\d{4}年\d{1,2}月\d{1,2}日)", content)
        check_out_match = re.search(r"(?:退房时间|离店时间|预计退房时间)(?:改为|改成|改到|调整为|变更为|为)(\d{4}[/-]\d{1,2}[/-]\d{1,2}|\d{4}年\d{1,2}月\d{1,2}日)", content)
        if check_in_match:
            updates["checkInDate"] = self._normalize_date_token(check_in_match.group(1))
        if check_out_match:
            updates["checkOutDate"] = self._normalize_date_token(check_out_match.group(1))

        room_type_keyword = None
        pair_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为|为)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)", content)
        if pair_match:
            room_type_keyword = pair_match.group(2)
        else:
            for pattern in (
                r"(?:房型|房间类型)(?:改为|改成|换成|换为|调整为|改到|换到|变更为|为)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)",
                r"(?:改为|改成|换成|换为|调整为|改到|换到|变更为)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)",
            ):
                room_type_match = re.search(pattern, content)
                if room_type_match:
                    room_type_keyword = room_type_match.group(1)
                    break
        if room_type_keyword:
            updates["roomTypeKeyword"] = room_type_keyword
        return self._compact_query_object(updates)

    def _extract_location_tokens(self, content: str) -> dict[str, Any]:
        location: dict[str, Any] = {}
        province_match = re.search(r"([\u4e00-\u9fa5]{2,12}(?:省|自治区|特别行政区))", content)
        city_match = re.search(r"([\u4e00-\u9fa5]{2,12}(?:市|州|地区|盟))", content)
        district_match = re.search(r"([\u4e00-\u9fa5]{2,12}(?:区|县|市))", content)
        street_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{2,20}(?:路|街|道|巷|里|镇))", content)
        landmark_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{2,20}(?:广场|商圈|地铁站|车站|机场|公园|景区|大学))", content)
        if province_match:
            location["province"] = self._normalize_location_token(province_match.group(1))
        if city_match:
            location["city"] = self._normalize_location_token(city_match.group(1))
        if district_match and not self._looks_like_city(district_match.group(1)):
            district = self._normalize_location_token(district_match.group(1))
            location.setdefault("district", district)
            location.setdefault("districtKeyword", district)
        if street_match:
            street = self._normalize_location_token(street_match.group(1))
            location.setdefault("street", street)
            location.setdefault("addressKeyword", street)
        if landmark_match:
            location.setdefault("addressKeyword", self._normalize_location_token(landmark_match.group(1)))

        location_candidate = self._extract_location_candidate(content, location)
        if location_candidate:
            if self._looks_like_city(location_candidate):
                location.setdefault("city", location_candidate)
            elif self._looks_like_district(location_candidate):
                location.setdefault("district", location_candidate)
                location.setdefault("districtKeyword", location_candidate)
            elif self._looks_like_street_or_landmark(location_candidate):
                if self._looks_like_street(location_candidate):
                    location.setdefault("street", location_candidate)
                location.setdefault("addressKeyword", location_candidate)
            elif not any(location.get(key) for key in ("city", "district", "addressKeyword")):
                location.setdefault("city", location_candidate)
        return self._compact_query_object(location)

    def _extract_location_candidate(self, content: str, location: dict[str, Any]) -> str | None:
        cleaned = content
        cleaned = re.sub(r"^(?:请|帮我|帮忙|给我|替我|我想|我要|麻烦你)", "", cleaned)
        cleaned = re.sub(r"^(?:查一下|查一查|查询|查找|搜索|搜一下|看一下|看看|推荐)", "", cleaned)
        cleaned = re.sub(r"(?:酒店|宾馆|民宿|客栈|住宿|旅馆|订单|预订)", "", cleaned)
        cleaned = re.sub(r"(?:附近|周边|的)", "", cleaned)
        cleaned = re.sub(r"(?i)wifi|wi-fi|breakfast|parking", "", cleaned)
        cleaned = cleaned.replace("无线网", "").replace("无线网络", "").replace("早餐", "").replace("停车场", "")
        cleaned = re.sub(r"([\u4e00-\u9fa5A-Za-z0-9]{1,8}房)", "", cleaned)
        cleaned = cleaned.strip("，。？！,.! ")
        if not cleaned:
            return None
        for value in location.values():
            if value:
                cleaned = cleaned.replace(str(value), "")
        cleaned = cleaned.strip("，。？！,.! ")
        return self._normalize_location_token(cleaned) if len(cleaned) >= 2 else None
    def _extract_hotel_name(self, content: str, location: dict[str, Any]) -> str | None:
        match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{2,24}(?:酒店|宾馆|民宿|客栈|公寓|度假村))", content)
        if match:
            candidate = match.group(1)
            stem = candidate
            for suffix in HOTEL_SUFFIXES:
                if stem.endswith(suffix):
                    stem = stem[: -len(suffix)]
                    break
            if stem and not any(value == stem for value in location.values()):
                return candidate
        for keyword in HOTEL_CHAIN_KEYWORDS:
            if keyword in content:
                return keyword if keyword.endswith(HOTEL_SUFFIXES) else f"{keyword}酒店"
        return None

    @staticmethod
    def _normalize_facility_text(content: str) -> str:
        normalized = (content or "").lower()
        normalized = normalized.replace("wi-fi", "wifi").replace("wi fi", "wifi").replace(" ", "")
        return normalized

    def _extract_facilities(self, content: str) -> dict[str, Any] | None:
        required: list[str] = []
        normalized_content = self._normalize_facility_text(content)
        for name, keywords in FACILITY_KEYWORDS.items():
            normalized_keywords = [self._normalize_facility_text(keyword) for keyword in keywords]
            if any(keyword in normalized_content for keyword in normalized_keywords):
                required.append(name)
        if not required:
            return None
        required = list(dict.fromkeys(required))
        match_mode = DEFAULT_FACILITY_MODE
        min_match_count: int | None = len(required)
        exact_count_match = re.search(r"(?:任意|至少)(\d+)个(?:即可|就行|满足)?", content)
        chinese_count_match = re.search(r"(?:任意|至少)(一个|两个|二个|三个)(?:即可|就行|满足)?", content)
        if exact_count_match:
            match_mode = "at_least"
            min_match_count = int(exact_count_match.group(1))
        elif chinese_count_match:
            mapping = {"一个": 1, "两个": 2, "二个": 2, "三个": 3}
            match_mode = "at_least"
            min_match_count = mapping.get(chinese_count_match.group(1), len(required))
        elif re.search(r"任意一个|任一|任意即可", content):
            match_mode = "any"
            min_match_count = 1
        elif re.search(r"全部满足|都满足|都要有|同时有|而且有|并且有|和", content):
            match_mode = "all"
            min_match_count = len(required)
        elif len(required) == 1:
            match_mode = "all"
            min_match_count = 1
        min_match_count = max(1, min(min_match_count or 1, len(required)))
        return {"required": required, "matchMode": match_mode, "minMatchCount": min_match_count}

    def _extract_room_type(self, content: str) -> dict[str, Any] | None:
        room_type_id_match = re.search(r"(?:roomTypeId|房型ID|房型编号)\s*[:：]?\s*(\d+)", content, re.IGNORECASE)
        result: dict[str, Any] = {}
        if room_type_id_match:
            result["roomTypeId"] = int(room_type_id_match.group(1))
        room_type_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{1,8}房)", content)
        if room_type_match:
            candidate = room_type_match.group(1)
            if candidate != ROOM_TYPE_SUFFIX and "酒店" not in candidate:
                result["roomTypeKeyword"] = candidate
        return result or None

    @staticmethod
    def _extract_price_range(content: str) -> tuple[float | None, float | None]:
        min_match = re.search(r"(\d+(?:\.\d+)?)\s*元?(?:以上|起|至少)", content)
        max_match = re.search(r"(\d+(?:\.\d+)?)\s*元?(?:以下|以内|不超过|最多)", content)
        range_match = re.search(r"(\d+(?:\.\d+)?)\s*[-到至]\s*(\d+(?:\.\d+)?)\s*元?", content)
        if range_match:
            return float(range_match.group(1)), float(range_match.group(2))
        return (float(min_match.group(1)) if min_match else None, float(max_match.group(1)) if max_match else None)

    @staticmethod
    def _extract_rating(content: str) -> float | None:
        rating_match = re.search(r"([1-5](?:\.\d)?)\s*(?:分|星)(?:以上|及以上)?", content)
        return float(rating_match.group(1)) if rating_match else None

    @staticmethod
    def _extract_explicit_order_id(content: str) -> int | None:
        match = re.search(r"(?:订单号?|orderId|reservationId)[:：#]?\s*(\d{1,18})", content, re.IGNORECASE)
        return int(match.group(1)) if match else None

    @staticmethod
    def _ordinal_token_to_index(token: str) -> int | None:
        if not token:
            return None
        if token.isdigit():
            return max(int(token) - 1, 0)
        mapping = {"一": 1, "二": 2, "两": 2, "三": 3, "四": 4, "五": 5, "六": 6, "七": 7, "八": 8, "九": 9, "十": 10}
        if token in mapping:
            return mapping[token] - 1
        if token.startswith("十") and token[1:] in mapping:
            return 10 + mapping[token[1:]] - 1
        if token.endswith("十") and token[:-1] in mapping:
            return mapping[token[:-1]] * 10 - 1
        return None

    def _extract_order_candidate_index(self, content: str) -> int | None:
        match = re.search(r"第([一二两三四五六七八九十\d]+)条", content)
        return self._ordinal_token_to_index(match.group(1)) if match else None

    def _resolve_candidate_order_query(self, content: str, guest_id: int, order_candidates: list[dict[str, Any]]) -> dict[str, Any] | None:
        if not order_candidates:
            return None
        explicit_order_id = self._extract_explicit_order_id(content)
        if explicit_order_id is not None:
            return {"guestId": guest_id, "reservationId": explicit_order_id, "limit": 1}
        candidate_index = self._extract_order_candidate_index(content)
        if candidate_index is None and any(token in content for token in ("这条订单", "该订单", "这笔订单", "上一条", "上一个", "最近预订的订单", "最近订单")):
            candidate_index = 0
        if candidate_index is None or candidate_index < 0 or candidate_index >= len(order_candidates):
            return None
        target = order_candidates[candidate_index] or {}
        target_id = target.get("reservationId") or target.get("orderId")
        return {"guestId": guest_id, "reservationId": int(target_id), "limit": 1} if target_id is not None else None

    @staticmethod
    def _extract_source_order_room_type(content: str) -> str | None:
        for pattern in (
            r"的([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为|为)",
            r"把([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为)",
            r"由([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为)",
        ):
            match = re.search(pattern, content)
            if match:
                return match.group(1)
        return None

    @staticmethod
    def _extract_cancel_reason(message: str) -> str | None:
        return "用户主动取消" if "不要了" in message else None

    def _detect_order_action(self, message: str) -> str | None:
        content = self._normalize_message(message)
        if not self._looks_like_order_request(content):
            return None
        if any(keyword in content for keyword in ORDER_CANCEL_HINTS):
            return ORDER_CANCEL_INTENT
        if any(keyword in content for keyword in ORDER_UPDATE_HINTS):
            return ORDER_UPDATE_INTENT
        return ORDER_QUERY_INTENT

    def _looks_like_order_request(self, message: str) -> bool:
        content = self._normalize_message(message)
        if not content:
            return False
        has_order_signal = any(keyword in content for keyword in ORDER_QUERY_HINTS) or content.startswith("帮我查最近订单")
        if not has_order_signal:
            return False
        if self._looks_like_hotel_search(content) and "订单" not in content and "预订" not in content and not any(keyword in content for keyword in ORDER_CANCEL_HINTS + ORDER_UPDATE_HINTS):
            return False
        return True

    @staticmethod
    def _extract_recent_days(content: str) -> int | None:
        days_match = re.search(r"最近(\d+)天", content)
        if days_match:
            return int(days_match.group(1))
        if "最近一周" in content or "最近7天" in content:
            return 7
        if "最近一个月" in content or "最近1个月" in content or "最近30天" in content:
            return 30
        return None

    @staticmethod
    def _extract_order_status(content: str) -> str | None:
        if any(keyword in content for keyword in ("已取消", "取消的订单")):
            return "cancelled"
        if any(keyword in content for keyword in ("已完成", "完成的订单")):
            return "completed"
        if any(keyword in content for keyword in ("已入住", "入住中的订单")):
            return "checked_in"
        return None
    def _looks_like_hotel_search(self, message: str) -> bool:
        content = self._normalize_message(message).lower()
        if any(keyword in content for keyword in HOTEL_SEARCH_HINTS):
            return True
        return any(keyword.lower() in content for keyword in HOTEL_CHAIN_KEYWORDS)

    @staticmethod
    def _looks_like_city(candidate: str) -> bool:
        return candidate.endswith(CITY_SUFFIXES)

    @staticmethod
    def _looks_like_district(candidate: str) -> bool:
        return candidate.endswith(DISTRICT_SUFFIXES)

    @staticmethod
    def _looks_like_street(candidate: str) -> bool:
        return candidate.endswith(("路", "街", "道", "巷", "里", "镇"))

    def _looks_like_street_or_landmark(self, candidate: str) -> bool:
        return candidate.endswith(LANDMARK_SUFFIXES)

    @staticmethod
    def _normalize_location_token(token: str) -> str:
        cleaned = (token or "").strip()
        cleaned = re.sub(r"市市$", "市", cleaned)
        cleaned = re.sub(r"区区$", "区", cleaned)
        cleaned = re.sub(r"县县$", "县", cleaned)
        cleaned = re.sub(r"路路$", "路", cleaned)
        return cleaned

    @staticmethod
    def _normalize_message(message: str) -> str:
        return re.sub(r"\s+", "", message or "")

    @staticmethod
    def _normalize_date_token(date_text: str) -> str:
        normalized = date_text.replace("年", "-").replace("月", "-").replace("日", "").replace("/", "-")
        year, month, day = normalized.split("-")
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"

    @staticmethod
    def _has_valid_hotel_condition(query: dict[str, Any]) -> bool:
        if not query:
            return False
        location = query.get("location") or {}
        facilities = query.get("facilities") or {}
        room_type = query.get("roomType") or {}
        return bool(location) or bool(facilities.get("required")) or bool(room_type) or any(query.get(key) not in (None, "", []) for key in ("hotelName", "minPrice", "maxPrice", "rating"))

    @staticmethod
    def _compact_query_object(source: Any) -> Any:
        if isinstance(source, dict):
            compacted = {key: ChatService._compact_query_object(value) for key, value in source.items()}
            return {key: value for key, value in compacted.items() if value not in (None, "", [], {})}
        if isinstance(source, list):
            compacted_list = [ChatService._compact_query_object(item) for item in source]
            return [item for item in compacted_list if item not in (None, "", [], {})]
        return source

    @staticmethod
    def _empty_hotel_structured_data() -> dict[str, Any]:
        return {"query": {}, "total": 0, "hotels": []}