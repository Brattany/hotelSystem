import hashlib
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional

try:
    from .chunker import split_text
    from .cleaner import clean_text
    from .knowledge_base import ChromaKnowledgeBase
    from .loaders import load_document
except ImportError:
    from chunker import split_text
    from cleaner import clean_text
    from knowledge_base import ChromaKnowledgeBase
    from loaders import load_document


DEFAULT_MODEL = "BAAI/bge-small-zh-v1.5"
DEFAULT_COLLECTION = "hotel_knowledge"
DEFAULT_PERSIST_DIR = "data/chroma"


def file_sha256(file_path: str | Path) -> str:
    sha = hashlib.sha256()
    with open(file_path, "rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            sha.update(block)
    return sha.hexdigest()


def ingest_file(
    file_path: str | Path,
    collection_name: str = DEFAULT_COLLECTION,
    persist_dir: str = DEFAULT_PERSIST_DIR,
    model_name: str = DEFAULT_MODEL,
    source: Optional[str] = None,
    extra_metadata: Optional[Dict[str, Any]] = None,
    chunk_size: int = 500,
    chunk_overlap: int = 100,
    replace_by_source: bool = True,
) -> Dict[str, Any]:
    """
    文件入库主流程：
    1. 加载
    2. 清洗
    3. 切片
    4. 向量化
    5. 写入 Chroma
    """
    path = Path(file_path)
    loaded = load_document(path)
    cleaned_text = clean_text(loaded.text)
    chunks = split_text(
        cleaned_text,
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
    )

    kb = ChromaKnowledgeBase(
        persist_dir=persist_dir,
        collection_name=collection_name,
        model_name=model_name,
    )

    source_name = source or loaded.metadata.get("source") or path.name

    # 同名文件重复上传时，先删旧块再插新块
    if replace_by_source:
        kb.delete_by_source(source_name)

    file_hash = file_sha256(path)
    now = datetime.now().isoformat(timespec="seconds")

    base_meta = {
        **loaded.metadata,
        "source": source_name,
        "file_hash": file_hash,
        "ingested_at": now,
    }

    if extra_metadata:
        base_meta.update(extra_metadata)

    ids = []
    documents = []
    metadatas = []

    for chunk in chunks:
        ids.append(f"{file_hash}_{chunk.chunk_index}")
        documents.append(chunk.text)
        metadatas.append(
            {
                **base_meta,
                "chunk_index": chunk.chunk_index,
                "start_char": chunk.start_char,
                "end_char": chunk.end_char,
            }
        )

    inserted = kb.upsert_chunks(
        documents=documents,
        metadatas=metadatas,
        ids=ids,
    )

    return {
        "file_name": path.name,
        "source": source_name,
        "collection_name": collection_name,
        "persist_dir": persist_dir,
        "chunk_count": inserted,
        "text_length": len(cleaned_text),
        "file_hash": file_hash,
    }