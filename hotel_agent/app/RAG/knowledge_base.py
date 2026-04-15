import json
from typing import Any, Dict, List, Optional

import chromadb

try:
    from .embedding import LocalEmbeddingModel
except ImportError:
    from embedding import LocalEmbeddingModel


Primitive = str | int | float | bool


class ChromaKnowledgeBase:
    def __init__(
        self,
        persist_dir: str = "data/chroma",
        collection_name: str = "hotel_knowledge",
        model_name: str = "BAAI/bge-small-zh-v1.5",
    ) -> None:
        self.client = chromadb.PersistentClient(path=persist_dir)
        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            metadata={"hnsw:space": "cosine"},
        )
        self.embedder = LocalEmbeddingModel(model_name=model_name)

    @staticmethod
    def _sanitize_metadata(metadata: Dict[str, Any]) -> Dict[str, Primitive]:
        """
        Chroma 的 metadata 只能存基础类型。
        """
        clean: Dict[str, Primitive] = {}
        for key, value in metadata.items():
            if value is None:
                clean[key] = ""
            elif isinstance(value, (str, int, float, bool)):
                clean[key] = value
            else:
                clean[key] = json.dumps(value, ensure_ascii=False)
        return clean

    def upsert_chunks(
        self,
        documents: List[str],
        metadatas: List[Dict[str, Any]],
        ids: List[str],
    ) -> int:
        if not documents:
            return 0

        embeddings = self.embedder.embed_documents(documents)
        metadatas = [self._sanitize_metadata(m) for m in metadatas]

        self.collection.upsert(
            ids=ids,
            documents=documents,
            metadatas=metadatas,
            embeddings=embeddings,
        )
        return len(ids)

    def query(
        self,
        query_text: str,
        top_k: int = 5,
        where: Optional[Dict[str, Any]] = None,
    ) -> List[Dict[str, Any]]:
        query_embedding = self.embedder.embed_query(query_text)

        result = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k,
            where=where,
            include=["documents", "metadatas", "distances"],
        )

        documents = result.get("documents", [[]])[0]
        metadatas = result.get("metadatas", [[]])[0]
        distances = result.get("distances", [[]])[0]
        ids = result.get("ids", [[]])[0]

        items = []
        for doc_id, doc, meta, distance in zip(ids, documents, metadatas, distances):
            items.append(
                {
                    "id": doc_id,
                    "document": doc,
                    "metadata": meta,
                    "distance": distance,
                }
            )
        return items

    def delete_by_source(self, source: str) -> int:
        existing = self.collection.get(where={"source": source}, include=[])
        ids = existing.get("ids", []) if existing else []
        if ids:
            self.collection.delete(ids=ids)
        return len(ids)

    def count(self) -> int:
        return self.collection.count()

    def list_sources(self) -> List[str]:
        result = self.collection.get(include=["metadatas"])
        metadatas = result.get("metadatas", []) if result else []
        sources = sorted(
            {m.get("source", "") for m in metadatas if m and m.get("source")}
        )
        return sources