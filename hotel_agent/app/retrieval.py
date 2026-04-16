from __future__ import annotations

from functools import lru_cache
from typing import Any

from .config import get_settings
from .llm_client import get_llm_client
import re


RAG_SYSTEM_PROMPT = """你是一名酒店知识库问答助手。

请严格基于给定的检索片段回答：
1. 优先引用检索片段中的信息，不要编造不存在的规则。
2. 如果片段信息不足，请明确说“当前知识库中没有足够信息”。
3. 回答尽量简洁、面向用户，可适度概括，但不要杜撰。
4. 如果有多个片段，请优先综合最相关的内容。
"""


def _distance_to_score(distance: Any) -> float | None:
    if distance is None:
        return None
    try:
        numeric_distance = float(distance)
    except (TypeError, ValueError):
        return None
    return round(max(0.0, min(1.0, 1 - numeric_distance)), 4)


def _load_knowledge_base_class():
    try:
        from .RAG.knowledge_base import ChromaKnowledgeBase
    except ImportError:  # pragma: no cover - fallback for script execution
        from .rag.knowledge_base import ChromaKnowledgeBase  # type: ignore
    return ChromaKnowledgeBase


class RetrievalService:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.client = None
        knowledge_base_class = _load_knowledge_base_class()
        self.knowledge_base = knowledge_base_class(
            persist_dir=self.settings.rag_persist_dir,
            collection_name=self.settings.rag_collection_name,
            model_name=self.settings.rag_embedding_model,
        )

    @staticmethod
    def _build_where(hotel_id: str | None = None, doc_type: str | None = None) -> dict[str, Any] | None:
        conditions: list[dict[str, Any]] = []
        if hotel_id:
            conditions.append({"hotel_id": str(hotel_id)})
        if doc_type:
            conditions.append({"doc_type": str(doc_type)})
        if not conditions:
            return None
        if len(conditions) == 1:
            return conditions[0]
        return {"$and": conditions}

    def search(
        self,
        query: str,
        *,
        hotel_id: str | None = None,
        doc_type: str | None = None,
        top_k: int | None = None,
    ) -> list[dict[str, Any]]:
        if not self.settings.rag_enabled:
            return []

        limit = top_k or self.settings.rag_top_k
        items = self.knowledge_base.query(
            query_text=query,
            top_k=limit,
            where=self._build_where(hotel_id=hotel_id, doc_type=doc_type),
        )

        normalized_items: list[dict[str, Any]] = []
        for item in items:
            metadata = item.get("metadata") or {}
            normalized_items.append(
                {
                    "id": item.get("id"),
                    "content": item.get("document") or "",
                    "source": metadata.get("source") or metadata.get("file_name") or "",
                    "file_name": metadata.get("file_name") or "",
                    "doc_type": metadata.get("doc_type") or "",
                    "hotel_id": metadata.get("hotel_id") or "",
                    "chunk_index": metadata.get("chunk_index"),
                    "distance": item.get("distance"),
                    "score": _distance_to_score(item.get("distance")),
                    "metadata": metadata,
                }
            )
        return normalized_items

    @staticmethod
    def _build_context(hits: list[dict[str, Any]], max_sources: int) -> str:
        blocks: list[str] = []
        for index, hit in enumerate(hits[:max_sources], start=1):
            source = hit.get("source") or hit.get("file_name") or f"片段{index}"
            content = (hit.get("content") or "").strip()
            if not content:
                continue
            blocks.append(f"[资料{index}] 来源：{source}\n{content}")
        return "\n\n".join(blocks)

    @staticmethod
    def _build_fast_answer(hits: list[dict[str, Any]]) -> str:
        parts: list[str] = []
        for hit in hits[:2]:
            content = re.sub(r"\s+", " ", (hit.get("content") or "")).strip()
            if not content:
                continue
            parts.append(content[:120])
        return "；".join(parts).strip()

    def answer(
        self,
        query: str,
        *,
        hotel_id: str | None = None,
        doc_type: str | None = None,
        top_k: int | None = None,
    ) -> dict[str, Any]:
        hits = self.search(query, hotel_id=hotel_id, doc_type=doc_type, top_k=top_k)
        if not hits:
            return {
                "ok": True,
                "route_type": "rag",
                "query": query,
                "answer": "当前知识库中还没有检索到相关内容。您可以换个问法，或者先上传对应知识文档。",
                "hits": [],
                "total": 0,
            }

        fast_answer = self._build_fast_answer(hits)
        short_context = len(hits) <= 2 and sum(len((hit.get("content") or "")) for hit in hits[:2]) <= 240

        # 快速路径：片段少且短，或未配置 LLM，直接返回轻量答案
        if short_context or not self.settings.llm_api_key:
            return {
                "ok": True,
                "route_type": "rag",
                "query": query,
                "answer": fast_answer or "已检索到相关知识片段，请查看来源后继续追问。",
                "hits": hits,
                "total": len(hits),
            }

        context = self._build_context(hits, self.settings.rag_max_sources)
        answer = ""
        try:
            completion = self._get_llm_client().chat.completions.create(
                model=self.settings.llm_model,
                messages=[
                    {"role": "system", "content": RAG_SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": f"用户问题：{query}\n\n知识片段：\n{context}\n\n请基于以上知识片段回答。",
                    },
                ],
            )
            answer = (completion.choices[0].message.content or "").strip()
        except Exception:
            answer = ""

        if not answer:
            answer = fast_answer or "我检索到了相关知识片段，但暂时没能生成稳定答案。您可以查看参考来源后继续追问。"

        return {
            "ok": True,
            "route_type": "rag",
            "query": query,
            "answer": answer,
            "hits": hits,
            "total": len(hits),
        }

    def _get_llm_client(self):
        if self.client is None:
            self.client = get_llm_client()
        return self.client


@lru_cache(maxsize=1)
def get_retrieval_service() -> RetrievalService:
    return RetrievalService()
