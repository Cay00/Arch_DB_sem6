from typing import TYPE_CHECKING, Literal

from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base

UserRole = Literal["Citizen", "Official"]

if TYPE_CHECKING:
    from app.models.issue import Issue


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[UserRole] = mapped_column(String(20), nullable=False, default="Citizen")
    issues: Mapped[list["Issue"]] = relationship(back_populates="user")
