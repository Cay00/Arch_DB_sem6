from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


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
