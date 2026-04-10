from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.deps import get_db
from app.core.security import hash_password
from app.models.user import User
from app.schemas.user import UserPublic, UserSyncRequest

router = APIRouter(prefix="/users", tags=["users"])


@router.post("", response_model=UserPublic)
def sync_user(
    payload: UserSyncRequest,
    response: Response,
    db: Session = Depends(get_db),
) -> User:
    """Upsert: aplikacja (Firebase) lub ponowna synchronizacja. 201 = utworzono, 200 = zaktualizowano."""
    uid = payload.firebase_uid
    u_by_uid = db.scalar(select(User).where(User.firebase_uid == uid)) if uid else None
    u_by_mail = db.scalar(select(User).where(User.email == payload.email))

    if u_by_uid and u_by_mail and u_by_uid.id != u_by_mail.id:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Firebase UID i e-mail wskazują na różne konta w bazie.",
        )

    user = u_by_uid or u_by_mail
    pw_plain = (payload.password_hash or "").strip() or None

    if user:
        if uid and user.firebase_uid and user.firebase_uid != uid:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="To konto jest już powiązane z innym Firebase UID.",
            )
        if uid:
            user.firebase_uid = uid
        if pw_plain:
            user.hashed_password = hash_password(pw_plain)
        if not user.display_name.strip():
            user.display_name = payload.email.split("@")[0]
        db.commit()
        db.refresh(user)
        response.status_code = status.HTTP_200_OK
        return user

    if not pw_plain and not uid:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Nowe konto wymaga firebase_uid lub hasła (password_hash).",
        )

    user = User(
        email=payload.email,
        firebase_uid=uid,
        hashed_password=hash_password(pw_plain) if pw_plain else None,
        display_name=payload.email.split("@")[0],
    )
    db.add(user)
    try:
        db.commit()
        db.refresh(user)
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="E-mail lub Firebase UID jest już zajęty.",
        ) from None
    response.status_code = status.HTTP_201_CREATED
    return user


@router.get("", response_model=list[UserPublic])
def list_users(db: Session = Depends(get_db)) -> list[User]:
    return list(db.scalars(select(User).order_by(User.id)))


@router.get("/by-email", response_model=UserPublic)
def get_user_by_email(email: str, db: Session = Depends(get_db)) -> User:
    user = db.scalar(select(User).where(User.email == email))
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nie znaleziono użytkownika.")
    return user
