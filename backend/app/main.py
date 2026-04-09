from fastapi import FastAPI
from sqlalchemy.exc import OperationalError

from app.api.routes import api_router
from app.core.config import settings
from app.database import Base, engine
from app.models import Issue, User

app = FastAPI(title=settings.APP_NAME, version=settings.APP_VERSION)
app.include_router(api_router)


@app.on_event("startup")
def create_tables() -> None:
    # Import models before create_all so SQLAlchemy sees all mapped tables.
    _ = (User, Issue)
    try:
        Base.metadata.create_all(bind=engine)
    except OperationalError as exc:
        raise RuntimeError(
            "Cannot connect to PostgreSQL. Check DATABASE_URL and database status."
        ) from exc


@app.get("/")
def root() -> dict[str, str]:
    return {"message": "UrbanFix API is running"}
