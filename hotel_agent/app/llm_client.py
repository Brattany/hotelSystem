from __future__ import annotations
from functools import lru_cache
from openai import OpenAI
from .config import get_settings

@lru_cache(maxsize=1)
def get_llm_client() -> OpenAI:
    settings = get_settings()
    if not settings.llm_api_key:
        raise RuntimeError("未配置 DEEPSEEK_API_KEY 或 OPENAI_API_KEY，无法调用大模型。")

    return OpenAI(
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
    )
