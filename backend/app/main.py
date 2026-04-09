from fastapi import FastAPI

from app.api.routes import api_router
from app.core.config import settings

app = FastAPI(title=settings.APP_NAME, version=settings.APP_VERSION)
app.include_router(api_router)


@app.get("/")
def root() -> dict[str, str]:
    return {"message": "UrbanFix API is running"}
