from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


def _read_env(*keys: str, default: str = "") -> str:
    for key in keys:
        value = os.getenv(key)
        if value is not None and value != "":
            return value
    return default


@dataclass(frozen=True)
class Settings:
    backend_base_url: str
    backend_internal_token: str
    backend_timeout_seconds: float
    llm_api_key: str
    llm_base_url: str
    llm_model: str
    llm_max_tool_rounds: int


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings(
        backend_base_url=_read_env("AGENT_BACKEND_BASE_URL", "BIZ_BACKEND_BASE_URL", default="http://localhost:8080"),
        backend_internal_token=_read_env("AGENT_BACKEND_INTERNAL_TOKEN", "BIZ_BACKEND_TOKEN", default=""),
        backend_timeout_seconds=float(_read_env("AGENT_BACKEND_TIMEOUT", "BIZ_BACKEND_TIMEOUT", default="10")),
        llm_api_key=_read_env("DEEPSEEK_API_KEY", "OPENAI_API_KEY", default=""),
        llm_base_url=_read_env("DEEPSEEK_BASE_URL", "OPENAI_BASE_URL", default="https://api.deepseek.com"),
        llm_model=_read_env("DEEPSEEK_MODEL", "OPENAI_MODEL", default="deepseek-chat"),
        llm_max_tool_rounds=int(_read_env("LLM_MAX_TOOL_ROUNDS", default="4")),
    )
