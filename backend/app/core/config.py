from pathlib import Path
from typing import Self

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

# Resolve `.env` next to `backend/` so settings load even when CWD is not `backend/`
_BACKEND_ROOT = Path(__file__).resolve().parent.parent.parent

_DEV_JWT_SECRET = "dev-jwt-secret-not-for-production"
_WEAK_JWT_SECRETS = frozenset(
    {
        _DEV_JWT_SECRET,
        "change-me-super-long-random-secret",
    }
)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=_BACKEND_ROOT / ".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    APP_NAME: str = "UrbanFix API"
    ENV: str = "development"
    DEBUG: bool = False

    DATABASE_URL: str = Field(default="sqlite:///./urbanfix.db")
    JWT_SECRET_KEY: str = Field(default=_DEV_JWT_SECRET)
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    @model_validator(mode="after")
    def reject_weak_secrets_in_production(self) -> Self:
        if self.ENV.lower() in ("production", "prod") and self.JWT_SECRET_KEY in _WEAK_JWT_SECRETS:
            raise ValueError(
                "Set JWT_SECRET_KEY to a strong random value when ENV is production "
                "(weak or placeholder secret detected)."
            )
        return self


settings = Settings()
