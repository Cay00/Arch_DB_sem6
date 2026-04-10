from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.issue import Issue
from app.models.user import User
from app.schemas.issue import IssueCreate, IssueResponse

router = APIRouter(prefix="/issues", tags=["issues"])


@router.post("", response_model=IssueResponse, status_code=status.HTTP_201_CREATED)
def create_issue(payload: IssueCreate, db: Session = Depends(get_db)) -> Issue:
    user = db.get(User, payload.user_id)
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found.",
        )

    issue = Issue(
        title=payload.title,
        description=payload.description,
        category=payload.category,
        status=payload.status,
        location=payload.location,
        user_id=payload.user_id,
    )
    db.add(issue)
    db.commit()
    db.refresh(issue)
    return issue


@router.get("", response_model=list[IssueResponse])
def list_issues(db: Session = Depends(get_db)) -> list[Issue]:
    return list(db.scalars(select(Issue).order_by(Issue.id.desc())))
