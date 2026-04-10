from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import api_router
from app.core.config import settings


@asynccontextmanager
async def lifespan(_app: FastAPI):
    from app.db.init_db import init_db

    init_db()
    yield


app = FastAPI(title=settings.APP_NAME, debug=settings.DEBUG, lifespan=lifespan)
app.include_router(api_router)
