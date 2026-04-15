from dataclasses import dataclass
from typing import List


@dataclass
class TextChunk:
    text: str
    chunk_index: int
    start_char: int
    end_char: int


SPLIT_SEPARATORS = (
    "\n\n",
    "\n",
    "。",
    "！",
    "？",
    "；",
    ". ",
    "! ",
    "? ",
    "; ",
    "，",
    ",",
)


def split_text(
    text: str,
    chunk_size: int = 500,
    chunk_overlap: int = 100,
    min_chunk_size: int | None = None,
) -> List[TextChunk]:
    """
    轻量级中文切片：
    - 按字符长度切
    - 优先在段落、句号、问号等自然边界断开
    - 保留 overlap，方便召回上下文
    """
    if not text:
        return []

    if chunk_overlap >= chunk_size:
        raise ValueError("chunk_overlap 必须小于 chunk_size")

    if min_chunk_size is None:
        min_chunk_size = max(80, chunk_size // 3)

    chunks: List[TextChunk] = []
    text_len = len(text)
    start = 0
    idx = 0

    while start < text_len:
        target_end = min(start + chunk_size, text_len)
        end = target_end

        if target_end < text_len:
            window = text[start:target_end]
            best_end = -1

            for sep in SPLIT_SEPARATORS:
                pos = window.rfind(sep)
                if pos >= min_chunk_size:
                    candidate_end = pos + len(sep)
                    if candidate_end > best_end:
                        best_end = candidate_end

            if best_end != -1:
                end = start + best_end

        chunk_text = text[start:end].strip()
        if chunk_text:
            chunks.append(
                TextChunk(
                    text=chunk_text,
                    chunk_index=idx,
                    start_char=start,
                    end_char=end,
                )
            )
            idx += 1

        if end >= text_len:
            break

        start = max(end - chunk_overlap, 0)
        while start < text_len and text[start].isspace():
            start += 1

    return chunks