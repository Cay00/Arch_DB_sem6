from datetime import datetime
from typing import TYPE_CHECKING, Literal

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base

IssueCategory = Literal["Drogi", "Zieleń", "Oświetlenie"]
IssueStatus = Literal["NEW", "IN_PROGRESS", "RESOLVED", "REJECTED"]

if TYPE_CHECKING:
    from app.models.user import User


class Issue(Base):
    __tablename__ = "issues"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str] = mapped_column(String(2000), nullable=False)
    category: Mapped[IssueCategory] = mapped_column(String(50), nullable=False)
    status: Mapped[IssueStatus] = mapped_column(String(20), nullable=False, default="NEW")
    location: Mapped[str] = mapped_column(String(255), nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    user: Mapped["User"] = relationship(back_populates="issues")
