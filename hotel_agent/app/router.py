from __future__ import annotations

from fastapi import APIRouter

from .chat_service import ChatService
from .schemas import ChatRequest, ChatResponse

router = APIRouter()


@router.get("/health")
def health() -> dict[str, bool]:
    return {"ok": True}


@router.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    try:
        chat_service = ChatService()
        return chat_service.chat(request)
    except Exception as exc:  # noqa: BLE001
        return ChatResponse(
            intent="general",
            structured_data={"route_type": "error"},
            reply=f"智能客服暂时不可用：{exc}",
            success=False,
            error=str(exc),
            used_tools=[],
        )
