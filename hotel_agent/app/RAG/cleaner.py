import re

CONTROL_CHARS_RE = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
ZERO_WIDTH_RE = re.compile(r"[\u200b\u200c\u200d\ufeff]")
MULTI_SPACE_RE = re.compile(r"[ \t]+")
MULTI_NEWLINE_RE = re.compile(r"\n{3,}")


def clean_text(text: str) -> str:
    """
    基础文本清洗：
    1. 统一换行
    2. 去除零宽字符、控制字符
    3. 合并多余空格
    4. 保留段落，但压缩多余空行
    """
    if not text:
        return ""

    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = text.replace("\u3000", " ")  # 全角空格转半角
    text = ZERO_WIDTH_RE.sub("", text)
    text = CONTROL_CHARS_RE.sub("", text)
    text = MULTI_SPACE_RE.sub(" ", text)
    text = re.sub(r"[ \t]+\n", "\n", text)

    lines = [line.strip() for line in text.split("\n")]
    cleaned_lines = []
    prev_blank = False

    for line in lines:
        if not line:
            if not prev_blank:
                cleaned_lines.append("")
            prev_blank = True
            continue
        cleaned_lines.append(line)
        prev_blank = False

    text = "\n".join(cleaned_lines).strip()
    text = MULTI_NEWLINE_RE.sub("\n\n", text)
    return text