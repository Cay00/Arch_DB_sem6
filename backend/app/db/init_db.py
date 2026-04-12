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


def _ensure_issue_votes_table() -> None:
    insp = inspect(engine)
    if insp.has_table("issue_votes"):
        return
    is_sqlite = engine.dialect.name == "sqlite"
    with engine.begin() as conn:
        if is_sqlite:
            conn.execute(
                text(
                    """
                    CREATE TABLE issue_votes (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        issue_id INTEGER NOT NULL,
                        user_id INTEGER NOT NULL,
                        value INTEGER NOT NULL,
                        CONSTRAINT fk_issue_votes_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
                        CONSTRAINT fk_issue_votes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                        CONSTRAINT uq_issue_vote_user UNIQUE (issue_id, user_id)
                    )
                    """
                )
            )
            conn.execute(text("CREATE INDEX ix_issue_votes_issue_id ON issue_votes (issue_id)"))
            conn.execute(text("CREATE INDEX ix_issue_votes_user_id ON issue_votes (user_id)"))
        else:
            conn.execute(
                text(
                    """
                    CREATE TABLE issue_votes (
                        id SERIAL PRIMARY KEY,
                        issue_id INTEGER NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        value INTEGER NOT NULL,
                        CONSTRAINT uq_issue_vote_user UNIQUE (issue_id, user_id)
                    )
                    """
                )
            )
            conn.execute(text("CREATE INDEX ix_issue_votes_issue_id ON issue_votes (issue_id)"))
            conn.execute(text("CREATE INDEX ix_issue_votes_user_id ON issue_votes (user_id)"))


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
    _ensure_issue_votes_table()
    _migrate_issue_status_legacy()
