from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache
from typing import Any

import httpx

from .config import get_settings


@dataclass
class BackendClientError(Exception):
    message: str
    status_code: int | None = None
    business_code: int | None = None
    payload: Any = None

    def __str__(self) -> str:
        return self.message


class BackendClient:
    def __init__(self, base_url: str, internal_token: str = "", timeout: float = 10.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.internal_token = internal_token
        self.timeout = timeout
        self._client = httpx.Client(timeout=self.timeout)

    def request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        json: dict[str, Any] | None = None,
    ) -> Any:
        url = f"{self.base_url}{path}"
        headers: dict[str, str] = {}
        if self.internal_token:
            headers["X-Internal-Token"] = self.internal_token

        try:
            response = self._client.request(
                method=method.upper(),
                url=url,
                params=params,
                json=json,
                headers=headers,
            )
        except httpx.TimeoutException as exc:
            raise BackendClientError("后端请求超时，请稍后重试") from exc
        except httpx.RequestError as exc:
            raise BackendClientError(f"连接后端失败: {exc}") from exc

        payload = None
        try:
            payload = response.json()
        except ValueError:
            if response.status_code >= 400:
                raise BackendClientError(
                    message=f"后端返回 HTTP {response.status_code}，且响应不是 JSON",
                    status_code=response.status_code,
                )
            raise BackendClientError("后端返回了无法解析的 JSON 响应", status_code=response.status_code)

        if response.status_code >= 400:
            raise BackendClientError(
                message=self._extract_message(payload, f"后端返回 HTTP {response.status_code}"),
                status_code=response.status_code,
                payload=payload,
            )

        if isinstance(payload, dict) and "code" in payload:
            business_code = payload.get("code")
            if business_code != 200:
                raise BackendClientError(
                    message=self._extract_message(payload, "后端业务处理失败"),
                    status_code=response.status_code,
                    business_code=business_code,
                    payload=payload,
                )
            return payload.get("data")

        return payload

    @staticmethod
    def _extract_message(payload: Any, fallback: str) -> str:
        if isinstance(payload, dict):
            for key in ("message", "msg", "error", "detail"):
                value = payload.get(key)
                if isinstance(value, str) and value.strip():
                    return value
        if isinstance(payload, str) and payload.strip():
            return payload
        return fallback


@lru_cache(maxsize=1)
def get_backend_client() -> BackendClient:
    settings = get_settings()
    return BackendClient(
        base_url=settings.backend_base_url,
        internal_token=settings.backend_internal_token,
        timeout=settings.backend_timeout_seconds,
    )
