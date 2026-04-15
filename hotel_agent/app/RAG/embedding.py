from typing import Iterable, List

from sentence_transformers import SentenceTransformer


class LocalEmbeddingModel:
    """
    本地向量模型封装。
    默认使用中文效果较好的 BGE 小模型。
    首次运行会自动下载模型。
    """

    def __init__(
        self,
        model_name: str = "BAAI/bge-small-zh-v1.5",
        device: str | None = None,
        normalize_embeddings: bool = True,
    ) -> None:
        self.model_name = model_name
        self.normalize_embeddings = normalize_embeddings
        self.model = SentenceTransformer(model_name, device=device)
        self.query_instruction = "为这个句子生成表示以用于检索相关文章："

    def _prepare_query(self, text: str) -> str:
        if self.model_name.lower().startswith("baai/bge"):
            return f"{self.query_instruction}{text}"
        return text

    def embed_documents(self, texts: Iterable[str]) -> List[List[float]]:
        vectors = self.model.encode(
            list(texts),
            batch_size=32,
            show_progress_bar=False,
            normalize_embeddings=self.normalize_embeddings,
        )
        return vectors.tolist()

    def embed_query(self, text: str) -> List[float]:
        vectors = self.model.encode(
            [self._prepare_query(text)],
            batch_size=1,
            show_progress_bar=False,
            normalize_embeddings=self.normalize_embeddings,
        )
        return vectors[0].tolist()