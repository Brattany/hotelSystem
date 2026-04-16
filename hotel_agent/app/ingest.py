from __future__ import annotations

from typing import Any

from .config import get_settings

try:
    from .RAG.ingest import ingest_file as rag_ingest_file
except ImportError:  # pragma: no cover - fallback for script execution
    from .rag.ingest import ingest_file as rag_ingest_file  # type: ignore


def ingest_document(
    file_path: str,
    *,
    source: str | None = None,
    extra_metadata: dict[str, Any] | None = None,
    chunk_size: int = 500,
    chunk_overlap: int = 100,
    replace_by_source: bool = True,
) -> dict[str, Any]:
    settings = get_settings()
    return rag_ingest_file(
        file_path=file_path,
        collection_name=settings.rag_collection_name,
        persist_dir=settings.rag_persist_dir,
        model_name=settings.rag_embedding_model,
        source=source,
        extra_metadata=extra_metadata,
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        replace_by_source=replace_by_source,
    )
