import os


class Settings:
    """Simple application settings loaded from environment."""

    APP_NAME: str = os.getenv("APP_NAME", "UrbanFix API")
    APP_VERSION: str = os.getenv("APP_VERSION", "0.1.0")
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "postgresql+psycopg://postgres:postgres@localhost:5432/urbanfix",
    )


settings = Settings()
