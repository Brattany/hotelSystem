from fastapi import FastAPI

from app.router import router


app = FastAPI(
    title="hotel_agent",
    version="1.0.0",
    description="Agent gateway for hotel backend tools.",
)

app.include_router(router)
