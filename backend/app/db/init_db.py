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


def _ensure_user_firebase_uid_column() -> None:
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "firebase_uid" in cols:
        return
    with engine.begin() as conn:
        conn.execute(text("ALTER TABLE users ADD COLUMN firebase_uid VARCHAR(128)"))
        conn.execute(
            text("CREATE UNIQUE INDEX IF NOT EXISTS ix_users_firebase_uid ON users (firebase_uid)")
        )


def _ensure_user_hashed_password_column() -> None:
    """Stary Postgres: kolumna `password_hash` zamiast `hashed_password`."""
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "hashed_password" in cols:
        return
    with engine.begin() as conn:
        conn.execute(text("ALTER TABLE users ADD COLUMN hashed_password VARCHAR(255)"))
        if "password_hash" in cols:
            conn.execute(
                text(
                    "UPDATE users SET hashed_password = password_hash "
                    "WHERE hashed_password IS NULL AND password_hash IS NOT NULL"
                )
            )


def _ensure_user_display_name_column() -> None:
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "display_name" in cols:
        return
    is_sqlite = engine.dialect.name == "sqlite"
    with engine.begin() as conn:
        if is_sqlite:
            conn.execute(text("ALTER TABLE users ADD COLUMN display_name VARCHAR(255) DEFAULT ''"))
        else:
            conn.execute(
                text("ALTER TABLE users ADD COLUMN display_name VARCHAR(255) NOT NULL DEFAULT ''")
            )
        conn.execute(
            text(
                """
                UPDATE users SET display_name = trim(first_name || ' ' || last_name)
                WHERE (display_name IS NULL OR display_name = '')
                  AND trim(coalesce(first_name, '') || ' ' || coalesce(last_name, '')) <> ''
                """
            )
        )
        if is_sqlite:
            conn.execute(
                text(
                    """
                    UPDATE users SET display_name = substr(email, 1, instr(email, '@') - 1)
                    WHERE display_name IS NULL OR display_name = ''
                    """
                )
            )
        else:
            conn.execute(
                text(
                    """
                    UPDATE users SET display_name = split_part(email, '@', 1)
                    WHERE display_name IS NULL OR display_name = ''
                    """
                )
            )


def _ensure_user_created_at_column() -> None:
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "created_at" in cols:
        return
    is_sqlite = engine.dialect.name == "sqlite"
    with engine.begin() as conn:
        if is_sqlite:
            conn.execute(
                text("ALTER TABLE users ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP")
            )
        else:
            conn.execute(
                text(
                    "ALTER TABLE users ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now()"
                )
            )


def _drop_legacy_user_columns() -> None:
    """Stary Postgres: `password_hash` i `role` (NOT NULL) blokują INSERT z nowego modelu."""
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    with engine.begin() as conn:
        if "password_hash" in cols:
            conn.execute(text("ALTER TABLE users DROP COLUMN password_hash"))
        if "role" in cols:
            conn.execute(text("ALTER TABLE users DROP COLUMN role"))


def _normalize_user_account_type_values() -> None:
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "account_type" not in cols:
        return
    with engine.begin() as conn:
        conn.execute(
            text(
                """
                UPDATE users SET account_type = lower(trim(account_type))
                WHERE account_type IS NOT NULL
                """
            )
        )
        conn.execute(
            text(
                """
                UPDATE users SET account_type = 'official'
                WHERE account_type IN ('urzednik', 'urzędnik', 'official')
                """
            )
        )
        conn.execute(
            text(
                """
                UPDATE users SET account_type = 'citizen'
                WHERE account_type IS NULL OR account_type = ''
                   OR account_type NOT IN ('citizen', 'official')
                """
            )
        )


def _migrate_user_role_to_account_type() -> None:
    """Stary Postgres: kolumna `role` zamiast `account_type`."""
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    cols = {c["name"] for c in insp.get_columns("users")}
    if "role" not in cols or "account_type" not in cols:
        return
    with engine.begin() as conn:
        conn.execute(
            text(
                """
                UPDATE users SET account_type = role
                WHERE role IS NOT NULL AND trim(role) <> ''
                  AND (account_type IS NULL OR account_type = 'citizen')
                """
            )
        )
        conn.execute(
            text(
                """
                UPDATE users SET account_type = 'official'
                WHERE lower(trim(role)) IN ('official', 'urzednik', 'urzędnik')
                """
            )
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


def _ensure_issue_coordinates_columns() -> None:
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    cols = {c["name"] for c in insp.get_columns("issues")}
    with engine.begin() as conn:
        if "location_lat" not in cols:
            conn.execute(text("ALTER TABLE issues ADD COLUMN location_lat FLOAT"))
        if "location_lng" not in cols:
            conn.execute(text("ALTER TABLE issues ADD COLUMN location_lng FLOAT"))


def _ensure_issue_status_timestamps_columns() -> None:
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    cols = {c["name"] for c in insp.get_columns("issues")}
    with engine.begin() as conn:
        if "reviewed_at" not in cols:
            conn.execute(text("ALTER TABLE issues ADD COLUMN reviewed_at TIMESTAMP"))
        if "accepted_at" not in cols:
            conn.execute(text("ALTER TABLE issues ADD COLUMN accepted_at TIMESTAMP"))
        if "rejected_at" not in cols:
            conn.execute(text("ALTER TABLE issues ADD COLUMN rejected_at TIMESTAMP"))


def _migrate_issue_status_legacy() -> None:
    """Stare zgłoszenia NEW → Zgłoszone."""
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    with engine.begin() as conn:
        conn.execute(text("UPDATE issues SET status = 'Zgłoszone' WHERE status IN ('NEW', 'new')"))


def _migrate_issue_categories_legacy() -> None:
    """Ujednolicenie historycznych wartości kategorii do wspólnego słownika."""
    insp = inspect(engine)
    if not insp.has_table("issues"):
        return
    with engine.begin() as conn:
        conn.execute(text("UPDATE issues SET category = 'Drogi' WHERE lower(trim(category)) IN ('drogi', 'droga')"))
        conn.execute(text("UPDATE issues SET category = 'Zieleń' WHERE lower(trim(category)) IN ('zielen', 'zieleń')"))
        conn.execute(
            text(
                "UPDATE issues SET category = 'Wandalizm' "
                "WHERE lower(trim(category)) IN ('wandalizm', 'akt wandalizmu')"
            )
        )
        conn.execute(
            text(
                "UPDATE issues SET category = 'Oświetlenie' "
                "WHERE lower(trim(category)) IN ('oswietlenie', 'oświetlenie')"
            )
        )
        conn.execute(
            text(
                "UPDATE issues SET category = 'Inwestycje' "
                "WHERE lower(trim(category)) IN ('inwestycje', 'inwestycja')"
            )
        )
        conn.execute(
            text(
                "UPDATE issues SET category = 'Porządek' "
                "WHERE lower(trim(category)) IN ('porzadek', 'porządek')"
            )
        )


def _migrate_reserved_user_emails() -> None:
    """Podmiana technicznego maila seedera z domeny specjalnej na poprawną."""
    insp = inspect(engine)
    if not insp.has_table("users"):
        return
    with engine.begin() as conn:
        conn.execute(
            text(
                "UPDATE users SET email = 'demo.seed@urbanfixdemo.com' "
                "WHERE lower(trim(email)) = 'demo.seed@urbanfix.local'"
            )
        )


def init_db() -> None:
    Base.metadata.create_all(bind=engine)
    _ensure_user_name_columns()
    _ensure_user_firebase_uid_column()
    _ensure_user_hashed_password_column()
    _ensure_user_display_name_column()
    _ensure_user_created_at_column()
    _ensure_user_account_type_column()
    _migrate_user_role_to_account_type()
    _normalize_user_account_type_values()
    _drop_legacy_user_columns()
    _ensure_issue_image_path_column()
    _ensure_issue_vote_count_column()
    _ensure_issue_votes_table()
    _ensure_issue_coordinates_columns()
    _ensure_issue_status_timestamps_columns()
    _migrate_issue_status_legacy()
    _migrate_issue_categories_legacy()
    _migrate_reserved_user_emails()
