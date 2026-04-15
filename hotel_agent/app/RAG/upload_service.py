import os
import shutil
from pathlib import Path
from typing import Any, Dict, Optional

from fastapi import APIRouter, FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

try:
    from .ingest import ingest_file
    from .knowledge_base import ChromaKnowledgeBase
except ImportError:
    from ingest import ingest_file
    from knowledge_base import ChromaKnowledgeBase


CHROMA_PERSIST_DIR = os.getenv("CHROMA_PERSIST_DIR", "data/chroma")
RAG_UPLOAD_DIR = Path(os.getenv("RAG_UPLOAD_DIR", "data/uploads"))
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
DEFAULT_COLLECTION = os.getenv("CHROMA_COLLECTION", "hotel_knowledge")
ALLOWED_SUFFIXES = {".txt", ".md", ".pdf", ".docx"}

RAG_UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

router = APIRouter(prefix="/rag", tags=["rag"])

# 既支持独立运行，也支持被主服务 include_router
app = FastAPI(title="RAG Upload Service")
app.include_router(router)


class QueryRequest(BaseModel):
    query: str = Field(..., description="用户问题")
    collection_name: str = Field(default=DEFAULT_COLLECTION)
    top_k: int = Field(default=5, ge=1, le=20)
    hotel_id: Optional[str] = None
    doc_type: Optional[str] = None


class DeleteSourceRequest(BaseModel):
    source: str
    collection_name: str = Field(default=DEFAULT_COLLECTION)


def _save_upload_file(upload_file: UploadFile) -> Path:
    suffix = Path(upload_file.filename).suffix.lower()
    if suffix not in ALLOWED_SUFFIXES:
        raise HTTPException(status_code=400, detail=f"不支持的文件类型: {suffix}")

    target_path = RAG_UPLOAD_DIR / upload_file.filename
    with target_path.open("wb") as out:
        shutil.copyfileobj(upload_file.file, out)
    return target_path


def _build_where(
    hotel_id: Optional[str],
    doc_type: Optional[str],
) -> Optional[Dict[str, Any]]:
    conditions = []
    if hotel_id:
        conditions.append({"hotel_id": hotel_id})
    if doc_type:
        conditions.append({"doc_type": doc_type})

    if not conditions:
        return None
    if len(conditions) == 1:
        return conditions[0]
    return {"$and": conditions}


@router.get("/health")
def health() -> Dict[str, Any]:
    return {"code": 200, "message": "ok"}


@router.post("/upload")
def upload_document(
    file: UploadFile = File(...),
    collection_name: str = Form(DEFAULT_COLLECTION),
    hotel_id: Optional[str] = Form(None),
    doc_type: Optional[str] = Form("policy"),
    chunk_size: int = Form(500),
    chunk_overlap: int = Form(100),
    replace_by_source: bool = Form(True),
) -> Dict[str, Any]:
    """
    上传文件 -> 保存本地 -> 清洗 -> 切片 -> 嵌入 -> 入 Chroma
    """
    try:
        saved_path = _save_upload_file(file)

        result = ingest_file(
            file_path=saved_path,
            collection_name=collection_name,
            persist_dir=CHROMA_PERSIST_DIR,
            model_name=EMBEDDING_MODEL,
            source=file.filename,
            extra_metadata={
                "hotel_id": hotel_id or "",
                "doc_type": doc_type or "policy",
            },
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            replace_by_source=replace_by_source,
        )

        return {
            "code": 200,
            "message": "上传并入库成功",
            "data": result,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"上传失败: {e}") from e


@router.post("/query")
def query_knowledge(req: QueryRequest) -> Dict[str, Any]:
    try:
        kb = ChromaKnowledgeBase(
            persist_dir=CHROMA_PERSIST_DIR,
            collection_name=req.collection_name,
            model_name=EMBEDDING_MODEL,
        )
        where = _build_where(req.hotel_id, req.doc_type)
        items = kb.query(req.query, top_k=req.top_k, where=where)

        return {
            "code": 200,
            "message": "查询成功",
            "data": items,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"查询失败: {e}") from e


@router.get("/sources")
def list_sources(collection_name: str = DEFAULT_COLLECTION) -> Dict[str, Any]:
    try:
        kb = ChromaKnowledgeBase(
            persist_dir=CHROMA_PERSIST_DIR,
            collection_name=collection_name,
            model_name=EMBEDDING_MODEL,
        )
        return {
            "code": 200,
            "message": "ok",
            "data": {
                "collection_name": collection_name,
                "count": kb.count(),
                "sources": kb.list_sources(),
            },
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"读取来源失败: {e}") from e


@router.post("/delete_source")
def delete_source(req: DeleteSourceRequest) -> Dict[str, Any]:
    try:
        kb = ChromaKnowledgeBase(
            persist_dir=CHROMA_PERSIST_DIR,
            collection_name=req.collection_name,
            model_name=EMBEDDING_MODEL,
        )
        deleted = kb.delete_by_source(req.source)
        return {
            "code": 200,
            "message": "删除成功",
            "data": {"deleted": deleted},
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除失败: {e}") from e


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("upload_service:app", host="0.0.0.0", port=9000, reload=True)