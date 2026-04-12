from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.api.routes import api_router
from app.core.config import BACKEND_ROOT, settings


@asynccontextmanager
async def lifespan(_app: FastAPI):
    from app.db.init_db import init_db

    init_db()
    yield


_upload_root = BACKEND_ROOT / settings.UPLOAD_DIR
_upload_root.mkdir(parents=True, exist_ok=True)

app = FastAPI(title=settings.APP_NAME, debug=settings.DEBUG, lifespan=lifespan)
app.include_router(api_router)
app.mount("/uploads", StaticFiles(directory=str(_upload_root)), name="uploads")
