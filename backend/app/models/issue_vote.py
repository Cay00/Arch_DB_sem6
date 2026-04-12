from sqlalchemy import ForeignKey, Integer, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base


class IssueVote(Base):
    __tablename__ = "issue_votes"
    __table_args__ = (UniqueConstraint("issue_id", "user_id", name="uq_issue_vote_user"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    issue_id: Mapped[int] = mapped_column(ForeignKey("issues.id", ondelete="CASCADE"), index=True, nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True, nullable=False)
    value: Mapped[int] = mapped_column(Integer, nullable=False)

    issue: Mapped["Issue"] = relationship("Issue", back_populates="votes")
    user: Mapped["User"] = relationship("User", back_populates="issue_votes")
