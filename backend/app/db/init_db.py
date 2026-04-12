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


def _ensure_user_account_type_column() -> None:
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "account_type" in cols:
        return
    is_sqlite = engine.dialect.name == "sqlite"
    with engine.begin() as conn:
        if is_sqlite:
            conn.execute(text("ALTER TABLE users ADD COLUMN account_type VARCHAR(20) DEFAULT 'citizen'"))
        else:
            conn.execute(
                text("ALTER TABLE users ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'citizen'")
            )


def _ensure_issue_vote_count_column() -> None:
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    cols = {c["name"] for c in insp.get_columns("issues")}
    if "vote_count" in cols:
        return
    with engine.begin() as conn:
        conn.execute(text("ALTER TABLE issues ADD COLUMN vote_count INTEGER NOT NULL DEFAULT 0"))


def _ensure_issue_image_path_column() -> None:
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    cols = {c["name"] for c in insp.get_columns("issues")}
    if "image_path" in cols:
        return
    is_sqlite = engine.dialect.name == "sqlite"
    with engine.begin() as conn:
        if is_sqlite:
            conn.execute(text("ALTER TABLE issues ADD COLUMN image_path VARCHAR(512)"))
        else:
            conn.execute(text("ALTER TABLE issues ADD COLUMN image_path VARCHAR(512)"))


def _migrate_issue_status_legacy() -> None:
    """Stare zgłoszenia NEW → Zgłoszone."""
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    with engine.begin() as conn:
        conn.execute(text("UPDATE issues SET status = 'Zgłoszone' WHERE status IN ('NEW', 'new')"))


def init_db() -> None:
    Base.metadata.create_all(bind=engine)
    _ensure_user_name_columns()
    _ensure_user_account_type_column()
    _ensure_issue_image_path_column()
    _ensure_issue_vote_count_column()
    _migrate_issue_status_legacy()
