import secrets

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.config import settings
from app.core.issue_status import DEFAULT_ON_CREATE
from app.models.issue import Issue
from app.models.user import User
from app.schemas.issue import IssueCreate, IssuePublic, IssueStatusUpdate

router = APIRouter(prefix="/issues", tags=["issues"])


def _require_official_by_email(db: Session, email: str) -> User:
    u = db.scalar(select(User).where(User.email == email.strip()))
    if u is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")
    if (u.account_type or "").lower() != "official":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Ta operacja wymaga konta Official.",
        )
    return u


def _list_all_issues(db: Session) -> list[Issue]:
    stmt = select(Issue).order_by(Issue.id.desc())
    return list(db.scalars(stmt))


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
def list_issues(
    user_id: int | None = None,
    official_email: str | None = None,
    issues_list_secret: str | None = Query(
        default=None,
        description=(
            "Opcjonalny token serwerowy: musi zgadzać się z ISSUES_LIST_SECRET w .env, "
            "wtedy zwracana jest pełna lista (np. webhook / n8n)."
        ),
    ),
    db: Session = Depends(get_db),
) -> list[Issue]:
    """Lista zgłoszeń.

    - `user_id` — zgłoszenia mieszkańca,
    - `official_email` — po weryfikacji konta Official, wszystkie zgłoszenia,
    - `issues_list_secret` — gdy w konfiguracji ustawiono `ISSUES_LIST_SECRET`, pełna lista dla integracji,
    - w `ENV=development` (bez zmiany domyślnej) samo `GET /issues` zwraca pełną listę (wygoda /docs),
    - w produkcji bez powyższych parametrów: 422.
    """
    if official_email is not None and official_email.strip():
        _require_official_by_email(db, official_email)
        return _list_all_issues(db)
    if user_id is not None:
        stmt = select(Issue).where(Issue.user_id == user_id).order_by(Issue.id.desc())
        return list(db.scalars(stmt))

    configured = (settings.ISSUES_LIST_SECRET or "").strip()
    if configured and issues_list_secret is not None:
        # compare_digest wymaga takiej samej długości — inaczej ValueError
        if len(configured) != len(issues_list_secret) or not secrets.compare_digest(
            configured, issues_list_secret
        ):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Nieprawidłowy issues_list_secret.",
            )
        return _list_all_issues(db)

    if settings.ENV.lower() in ("development", "dev"):
        return _list_all_issues(db)

    raise HTTPException(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        detail=(
            "Podaj user_id (swoje zgłoszenia), official_email (widok Official), "
            "albo issues_list_secret (gdy skonfigurowano ISSUES_LIST_SECRET)."
        ),
    )


@router.get("/{issue_id}", response_model=IssuePublic)
def get_issue(
    issue_id: int,
    viewer_email: str = Query(..., description="E-mail zalogowanego użytkownika (do weryfikacji uprawnień)"),
    db: Session = Depends(get_db),
) -> Issue:
    viewer = db.scalar(select(User).where(User.email == viewer_email.strip()))
    if viewer is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")
    issue = db.get(Issue, issue_id)
    if issue is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono zgłoszenia.")
    if (viewer.account_type or "").lower() != "official" and issue.user_id != viewer.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Brak dostępu do tego zgłoszenia.",
        )
    return issue


@router.patch("/{issue_id}", response_model=IssuePublic)
def update_issue_status(
    issue_id: int,
    payload: IssueStatusUpdate,
    db: Session = Depends(get_db),
) -> Issue:
    actor = db.scalar(select(User).where(User.email == payload.actor_email))
    if actor is None or (actor.account_type or "").lower() != "official":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Tylko konto Official może zmieniać status zgłoszenia.",
        )
    issue = db.get(Issue, issue_id)
    if issue is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono zgłoszenia.")
    issue.status = payload.status
    db.commit()
    db.refresh(issue)
    return issue
