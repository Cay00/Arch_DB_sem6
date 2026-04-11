from sqlalchemy import inspect, text

from app.db.base import Base
from app.db.session import engine


def _ensure_user_name_columns() -> None:
    """Stare pliki SQLite/Postgres bez kolumn imię/nazwisko — create_all nie robi ALTER."""
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "first_name" in cols and "last_name" in cols:
        return
    is_sqlite = engine.dialect.name == "sqlite"
    with engine.begin() as conn:
        if "first_name" not in cols:
            if is_sqlite:
                conn.execute(text("ALTER TABLE users ADD COLUMN first_name VARCHAR(120) DEFAULT ''"))
            else:
                conn.execute(
                    text("ALTER TABLE users ADD COLUMN first_name VARCHAR(120) NOT NULL DEFAULT ''")
                )
        if "last_name" not in cols:
            if is_sqlite:
                conn.execute(text("ALTER TABLE users ADD COLUMN last_name VARCHAR(120) DEFAULT ''"))
            else:
                conn.execute(
                    text("ALTER TABLE users ADD COLUMN last_name VARCHAR(120) NOT NULL DEFAULT ''")
                )


def init_db() -> None:
    Base.metadata.create_all(bind=engine)
    _ensure_user_name_columns()
