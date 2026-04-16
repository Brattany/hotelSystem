from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


def _read_env(*keys: str, default: str = "") -> str:
    for key in keys:
        value = os.getenv(key)
        if value is not None and value != "":
            return value
    return default


def _read_bool_env(*keys: str, default: bool = False) -> bool:
    raw_value = _read_env(*keys, default=str(default).lower())
    return raw_value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    backend_base_url: str
    backend_internal_token: str
    backend_timeout_seconds: float
    llm_api_key: str
    llm_base_url: str
    llm_model: str
    llm_max_tool_rounds: int
    rag_enabled: bool
    rag_persist_dir: str
    rag_collection_name: str
    rag_embedding_model: str
    rag_upload_dir: str
    rag_top_k: int
    rag_mix_top_k: int
    rag_max_sources: int


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    project_root = Path(__file__).resolve().parents[2]
    default_rag_dir = project_root / "data" / "chroma"
    default_upload_dir = project_root / "data" / "uploads"

    return Settings(
        backend_base_url=_read_env("AGENT_BACKEND_BASE_URL", "BIZ_BACKEND_BASE_URL", default="http://localhost:8080"),
        backend_internal_token=_read_env("AGENT_BACKEND_INTERNAL_TOKEN", "BIZ_BACKEND_TOKEN", default=""),
        backend_timeout_seconds=float(_read_env("AGENT_BACKEND_TIMEOUT", "BIZ_BACKEND_TIMEOUT", default="10")),
        llm_api_key=_read_env("DEEPSEEK_API_KEY", "OPENAI_API_KEY", default=""),
        llm_base_url=_read_env("DEEPSEEK_BASE_URL", "OPENAI_BASE_URL", default="https://api.deepseek.com"),
        llm_model=_read_env("DEEPSEEK_MODEL", "OPENAI_MODEL", default="deepseek-chat"),
        llm_max_tool_rounds=int(_read_env("LLM_MAX_TOOL_ROUNDS", default="4")),
        rag_enabled=_read_bool_env("RAG_ENABLED", default=True),
        rag_persist_dir=_read_env("CHROMA_PERSIST_DIR", default=str(default_rag_dir)),
        rag_collection_name=_read_env("CHROMA_COLLECTION", default="hotel_knowledge"),
        rag_embedding_model=_read_env("EMBEDDING_MODEL", default="BAAI/bge-small-zh-v1.5"),
        rag_upload_dir=_read_env("RAG_UPLOAD_DIR", default=str(default_upload_dir)),
        rag_top_k=int(_read_env("RAG_TOP_K", default="5")),
        rag_mix_top_k=int(_read_env("RAG_MIX_TOP_K", default="3")),
        rag_max_sources=int(_read_env("RAG_MAX_SOURCES", default="4")),
    )
