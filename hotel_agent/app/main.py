from fastapi import FastAPI
from fastapi.responses import JSONResponse

from app.router import router
from app.RAG.upload_service import router as rag_router


class UTF8JSONResponse(JSONResponse):
    media_type = "application/json; charset=utf-8"


app = FastAPI(
    title="hotel_agent",
    version="1.0.0",
    description="Agent gateway for hotel backend tools.",
    default_response_class=UTF8JSONResponse,
)

app.include_router(router)
app.include_router(rag_router)
