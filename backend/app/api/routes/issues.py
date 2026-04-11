from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.issue_status import DEFAULT_ON_CREATE
from app.models.issue import Issue
from app.models.user import User
from app.schemas.issue import IssueCreate, IssuePublic

router = APIRouter(prefix="/issues", tags=["issues"])


@router.post("", response_model=IssuePublic, status_code=status.HTTP_201_CREATED)
def create_issue(payload: IssueCreate, db: Session = Depends(get_db)) -> Issue:
    user = db.get(User, payload.user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")

    issue = Issue(
        title=payload.title,
        description=payload.description,
        category=payload.category,
        status=DEFAULT_ON_CREATE,
        location=payload.location,
        user_id=payload.user_id,
    )
    db.add(issue)
    db.commit()
    db.refresh(issue)
    return issue


@router.get("", response_model=list[IssuePublic])
def list_issues(user_id: int | None = None, db: Session = Depends(get_db)) -> list[Issue]:
    stmt = select(Issue)
    if user_id is not None:
        stmt = stmt.where(Issue.user_id == user_id)
    stmt = stmt.order_by(Issue.id.desc())
    return list(db.scalars(stmt))
