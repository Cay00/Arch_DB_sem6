from datetime import datetime
from typing import TYPE_CHECKING, Literal

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.core.issue_status import ALL_STATUSES

if TYPE_CHECKING:
    from app.models.issue import Issue


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
    #: Publiczny URL zdjęcia (wg ``API_PUBLIC_BASE_URL``) lub ścieżka ``/uploads/...`` — pole w odpowiedzi ``GET /issues`` (webhook).
    image_url: str | None = None
    #: Suma głosów (+1 / −1) — wynik netto społeczności.
    vote_count: int = 0
    #: Głos zalogowanego użytkownika (+1 lub −1), jeśli już zagłosował; w przeciwnym razie ``null``.
    viewer_vote: int | None = None
    created_at: datetime


def issue_to_public(issue: "Issue", *, viewer_vote: int | None = None) -> IssuePublic:
    return IssuePublic(
        id=issue.id,
        title=issue.title,
        description=issue.description,
        category=issue.category,
        status=issue.status,
        location=issue.location,
        user_id=issue.user_id,
        image_url=issue.image_url,
        vote_count=issue.vote_count,
        viewer_vote=viewer_vote,
        created_at=issue.created_at,
    )


class IssueVoteCreate(BaseModel):
    user_id: int
    value: Literal[1, -1]


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
