from pydantic import BaseModel, ConfigDict

from app.models.report import ReportStatus


class ReportCreate(BaseModel):
    title: str
    description: str
    location: str


class ReportRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str
    location: str
    status: ReportStatus
    reporter_id: int
