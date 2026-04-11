from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator, model_validator


class UserSyncRequest(BaseModel):
    """Sync z aplikacji (Firebase + opcjonalnie hasło). Pole `password_hash` to nadal czyste hasło z klienta — hashujemy w serwerze."""

    email: EmailStr
    firebase_uid: str | None = Field(default=None, max_length=128)
    password_hash: str | None = Field(default=None, max_length=500)
    first_name: str | None = Field(default=None, max_length=120)
    last_name: str | None = Field(default=None, max_length=120)
    account_type: str | None = Field(default=None, max_length=20)

    model_config = ConfigDict(extra="ignore")

    @field_validator("account_type", mode="before")
    @classmethod
    def normalize_account_type(cls, v: object) -> str | None:
        if v is None:
            return None
        if isinstance(v, str) and not v.strip():
            return None
        s = str(v).strip().lower()
        if s not in ("citizen", "official"):
            raise ValueError("account_type musi być citizen lub official")
        return s

    @model_validator(mode="after")
    def need_identifier(self) -> "UserSyncRequest":
        if not self.firebase_uid and not (self.password_hash and self.password_hash.strip()):
            raise ValueError("Podaj firebase_uid albo password_hash")
        return self


class UserPublic(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    first_name: str
    last_name: str
    display_name: str
    account_type: str
    firebase_uid: str | None = None
