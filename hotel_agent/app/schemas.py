from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field, model_validator


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str = Field(..., description="用户输入")
    guest_id: int | None = Field(default=None, description="当前登录用户 guestId")
    user_id: int | None = Field(default=None, description="兼容旧字段，等同 guestId")
    route_hint: str | None = Field(default=None, description="可选路由提示：structured / rag / hybrid / auto")
    hotel_id: str | None = Field(default=None, description="RAG 检索时可选的酒店过滤条件")
    doc_type: str | None = Field(default=None, description="RAG 检索时可选的文档类型过滤条件")
    top_k: int | None = Field(default=None, description="RAG 检索返回的最大片段数")
    history: list[ChatMessage] = Field(default_factory=list, description="可选的历史消息")
    order_candidates: list[dict[str, Any]] = Field(default_factory=list, description="当前会话中待确认的订单候选列表")

    @model_validator(mode="after")
    def sync_guest_id(self) -> "ChatRequest":
        if self.guest_id is None and self.user_id is not None:
            self.guest_id = self.user_id
        return self


class ToolExecutionRecord(BaseModel):
    tool_name: str
    tool_args: dict[str, Any]
    tool_result: dict[str, Any]


class KnowledgeHit(BaseModel):
    id: str | None = None
    content: str = ""
    source: str = ""
    file_name: str = ""
    doc_type: str = ""
    hotel_id: str = ""
    chunk_index: int | None = None
    score: float | None = None
    distance: float | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class ChatResponse(BaseModel):
    intent: str
    structured_data: dict[str, Any] = Field(default_factory=dict)
    reply: str
    success: bool
    error: str | None = None
    used_tools: list[ToolExecutionRecord] = Field(default_factory=list)
