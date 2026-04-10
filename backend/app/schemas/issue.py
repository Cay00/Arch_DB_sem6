from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class IssueCreate(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str = Field(min_length=1, max_length=4000)
    category: str = Field(min_length=1, max_length=50)
    location: str = Field(min_length=1, max_length=255)
    user_id: int
    status: str = Field(default="NEW", max_length=20)


class IssuePublic(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str
    category: str
    status: str
    location: str
    user_id: int
    created_at: datetime
