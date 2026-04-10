from pydantic import BaseModel, ConfigDict, EmailStr, Field, model_validator


class UserSyncRequest(BaseModel):
    """Sync z aplikacji (Firebase + opcjonalnie hasło). Pole `password_hash` to nadal czyste hasło z klienta — hashujemy w serwerze."""

    email: EmailStr
    firebase_uid: str | None = Field(default=None, max_length=128)
    password_hash: str | None = Field(default=None, max_length=500)

    model_config = ConfigDict(extra="ignore")

    @model_validator(mode="after")
    def need_identifier(self) -> "UserSyncRequest":
        if not self.firebase_uid and not (self.password_hash and self.password_hash.strip()):
            raise ValueError("Podaj firebase_uid albo password_hash")
        return self


class UserPublic(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    display_name: str
    firebase_uid: str | None = None
