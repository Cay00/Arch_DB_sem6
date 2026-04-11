from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.core.issue_status import ALL_STATUSES


class IssueCreate(BaseModel):
    """Status ustawia serwer zawsze na „Zgłoszone”; pole `status` z klienta jest ignorowane (extra=ignore)."""

    model_config = ConfigDict(extra="ignore")

    title: str = Field(min_length=1, max_length=255)
    description: str = Field(min_length=1, max_length=4000)
    category: str = Field(min_length=1, max_length=50)
    location: str = Field(min_length=1, max_length=255)
    user_id: int


class IssuePublic(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str
    category: str
    status: str = Field(max_length=32)
    location: str
    user_id: int
    created_at: datetime


class IssueStatusUpdate(BaseModel):
    """Zmiana statusu — wyłącznie przez konto Official (weryfikacja po actor_email)."""

    status: str = Field(max_length=32)
    actor_email: EmailStr

    @field_validator("status")
    @classmethod
    def status_must_be_allowed(cls, v: str) -> str:
        if v not in ALL_STATUSES:
            raise ValueError(f"status musi być jednym z: {', '.join(sorted(ALL_STATUSES))}")
        return v
