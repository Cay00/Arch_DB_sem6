import secrets

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.config import settings
from app.core.issue_status import DEFAULT_ON_CREATE, ZGLOSZONE
from app.core.media import save_issue_upload
from app.models.issue import Issue
from app.models.issue_vote import IssueVote
from app.models.user import User
from app.schemas.issue import (
    IssueCreate,
    IssuePublic,
    IssueStatusUpdate,
    IssueVoteCreate,
    issue_to_public,
)

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


def _list_community_submitted(db: Session) -> list[Issue]:
    stmt = select(Issue).where(Issue.status == ZGLOSZONE).order_by(Issue.id.desc())
    return list(db.scalars(stmt))


def _serialize_issues(db: Session, issues: list[Issue], viewer_user_id: int | None) -> list[IssuePublic]:
    if not issues:
        return []
    if viewer_user_id is None:
        return [issue_to_public(i) for i in issues]
    ids = [i.id for i in issues]
    stmt = select(IssueVote.issue_id, IssueVote.value).where(
        IssueVote.user_id == viewer_user_id,
        IssueVote.issue_id.in_(ids),
    )
    m = {row[0]: row[1] for row in db.execute(stmt)}
    return [issue_to_public(i, viewer_vote=m.get(i.id)) for i in issues]


@router.post("", response_model=IssuePublic, status_code=status.HTTP_201_CREATED)
def create_issue(
    title: str = Form(...),
    description: str = Form(...),
    category: str = Form(...),
    location: str = Form(...),
    user_id: int = Form(...),
    image: UploadFile | None = File(None),
    db: Session = Depends(get_db),
) -> IssuePublic:
    """Tworzenie zgłoszenia (multipart/form-data — zgodnie z aplikacją mobilną, pole pliku ``image`` opcjonalne)."""
    try:
        payload = IssueCreate(
            title=title.strip(),
            description=description.strip(),
            category=category.strip(),
            location=location.strip(),
            user_id=user_id,
        )
    except ValidationError as e:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=e.errors())

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
    db.flush()

    if image is not None and (image.filename or "").strip():
        try:
            issue.image_path = save_issue_upload(issue.id, image)
        except ValueError as e:
            db.rollback()
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e)) from e

    db.commit()
    db.refresh(issue)
    return issue_to_public(issue)


@router.get("", response_model=list[IssuePublic])
def list_issues(
    user_id: int | None = None,
    official_email: str | None = None,
    community_viewer_email: str | None = Query(
        default=None,
        description=(
            "E-mail zalogowanego użytkownika: zwraca zgłoszenia w statusie „Zgłoszone” "
            "(widok społeczności w aplikacji). Konto musi istnieć w bazie."
        ),
    ),
    issues_list_secret: str | None = Query(
        default=None,
        description=(
            "Opcjonalny token serwerowy: musi zgadzać się z ISSUES_LIST_SECRET w .env, "
            "wtedy zwracana jest pełna lista (np. webhook / n8n)."
        ),
    ),
    db: Session = Depends(get_db),
) -> list[IssuePublic]:
    """Lista zgłoszeń.

    - `user_id` — zgłoszenia mieszkańca,
    - `official_email` — po weryfikacji konta Official, wszystkie zgłoszenia,
    - `community_viewer_email` — po weryfikacji, że użytkownik istnieje: wszystkie zgłoszenia w statusie „Zgłoszone” (widok społeczności),
    - `issues_list_secret` — gdy w konfiguracji ustawiono `ISSUES_LIST_SECRET`, pełna lista dla integracji (JSON z polem `image_url` przy zgłoszeniu ze zdjęciem),
    - w `ENV=development` (bez zmiany domyślnej) samo `GET /issues` zwraca pełną listę (wygoda /docs),
    - w produkcji bez powyższych parametrów: 422.
    """
    if official_email is not None and official_email.strip():
        off = _require_official_by_email(db, official_email)
        issues = _list_all_issues(db)
        return _serialize_issues(db, issues, off.id)
    if user_id is not None:
        stmt = select(Issue).where(Issue.user_id == user_id).order_by(Issue.id.desc())
        issues = list(db.scalars(stmt))
        return _serialize_issues(db, issues, user_id)

    if community_viewer_email is not None and community_viewer_email.strip():
        v = db.scalar(select(User).where(User.email == community_viewer_email.strip()))
        if v is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")
        issues = _list_community_submitted(db)
        return _serialize_issues(db, issues, v.id)

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
        issues = _list_all_issues(db)
        return _serialize_issues(db, issues, None)

    if settings.ENV.lower() in ("development", "dev"):
        issues = _list_all_issues(db)
        return _serialize_issues(db, issues, None)

    raise HTTPException(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        detail=(
            "Podaj user_id (swoje zgłoszenia), official_email (widok Official), "
            "community_viewer_email (zgłoszenia „Zgłoszone” — społeczność), "
            "albo issues_list_secret (gdy skonfigurowano ISSUES_LIST_SECRET)."
        ),
    )


@router.post("/{issue_id}/vote", response_model=IssuePublic)
def cast_issue_vote(
    issue_id: int,
    payload: IssueVoteCreate,
    db: Session = Depends(get_db),
) -> IssuePublic:
    voter = db.get(User, payload.user_id)
    if voter is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")
    issue = db.get(Issue, issue_id)
    if issue is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono zgłoszenia.")
    is_official = (voter.account_type or "").lower() == "official"
    is_owner = issue.user_id == voter.id
    is_public_submitted = issue.status == ZGLOSZONE
    if not is_official and not is_owner and not is_public_submitted:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Brak dostępu do głosowania w tym zgłoszeniu.",
        )
    existing = db.scalar(
        select(IssueVote).where(IssueVote.issue_id == issue_id, IssueVote.user_id == payload.user_id)
    )
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Już oddano głos na to zgłoszenie.",
        )
    db.add(IssueVote(issue_id=issue_id, user_id=payload.user_id, value=payload.value))
    issue.vote_count += payload.value
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Już oddano głos na to zgłoszenie.",
        ) from None
    db.refresh(issue)
    return issue_to_public(issue, viewer_vote=payload.value)


@router.get("/{issue_id}", response_model=IssuePublic)
def get_issue(
    issue_id: int,
    viewer_email: str = Query(..., description="E-mail zalogowanego użytkownika (do weryfikacji uprawnień)"),
    db: Session = Depends(get_db),
) -> IssuePublic:
    viewer = db.scalar(select(User).where(User.email == viewer_email.strip()))
    if viewer is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")
    issue = db.get(Issue, issue_id)
    if issue is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono zgłoszenia.")
    is_official = (viewer.account_type or "").lower() == "official"
    is_owner = issue.user_id == viewer.id
    is_public_submitted = issue.status == ZGLOSZONE
    if not is_official and not is_owner and not is_public_submitted:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Brak dostępu do tego zgłoszenia.",
        )
    viewer_vote = db.scalar(
        select(IssueVote.value).where(
            IssueVote.issue_id == issue.id,
            IssueVote.user_id == viewer.id,
        )
    )
    return issue_to_public(issue, viewer_vote=viewer_vote)


@router.patch("/{issue_id}", response_model=IssuePublic)
def update_issue_status(
    issue_id: int,
    payload: IssueStatusUpdate,
    db: Session = Depends(get_db),
) -> IssuePublic:
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
    av = db.scalar(
        select(IssueVote.value).where(IssueVote.issue_id == issue.id, IssueVote.user_id == actor.id),
    )
    return issue_to_public(issue, viewer_vote=av)
