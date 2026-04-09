from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict

IssueCategory = Literal["Drogi", "Zieleń", "Oświetlenie"]
IssueStatus = Literal["NEW", "IN_PROGRESS", "RESOLVED", "REJECTED"]


class IssueCreate(BaseModel):
    title: str
    description: str
    category: IssueCategory
    location: str
    user_id: int
    status: IssueStatus = "NEW"


class IssueResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str
    category: IssueCategory
    status: IssueStatus
    location: str
    user_id: int
    created_at: datetime
