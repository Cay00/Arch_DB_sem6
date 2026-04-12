"""Zapis zdjęć zgłoszeń i budowa publicznego URL (lista / webhook)."""

from pathlib import Path

from fastapi import UploadFile

from app.core.config import BACKEND_ROOT, settings

MAX_ISSUE_IMAGE_BYTES = 5 * 1024 * 1024
ALLOWED_IMAGE_TYPES = frozenset({"image/jpeg", "image/png", "image/webp"})


def issue_files_dir() -> Path:
    d = BACKEND_ROOT / settings.UPLOAD_DIR / "issues"
    d.mkdir(parents=True, exist_ok=True)
    return d


def public_issue_image_url(image_path: str | None) -> str | None:
    if not image_path:
        return None
    rel = f"/uploads/{image_path.lstrip('/')}"
    base = (settings.API_PUBLIC_BASE_URL or "").strip().rstrip("/")
    return f"{base}{rel}" if base else rel


def save_issue_upload(issue_id: int, upload: UploadFile) -> str:
    """Zwraca ścieżkę względem katalogu uploads, np. ``issues/3.jpg``."""
    ctype = (upload.content_type or "").split(";")[0].strip().lower()
    if ctype and ctype not in ALLOWED_IMAGE_TYPES:
        raise ValueError(f"Niedozwolony typ pliku: {ctype}")

    ext = Path(upload.filename or "").suffix.lower()
    if ext not in {".jpg", ".jpeg", ".png", ".webp"}:
        ext = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}.get(ctype, ".jpg")

    dest_name = f"{issue_id}{ext}"
    dest = issue_files_dir() / dest_name
    total = 0
    try:
        upload.file.seek(0)
    except (AttributeError, OSError):
        pass
    with dest.open("wb") as out:
        while True:
            chunk = upload.file.read(1024 * 64)
            if not chunk:
                break
            total += len(chunk)
            if total > MAX_ISSUE_IMAGE_BYTES:
                raise ValueError("Zdjęcie przekracza limit 5 MB.")
            out.write(chunk)
    if total == 0:
        dest.unlink(missing_ok=True)
        raise ValueError("Pusty plik zdjęcia.")
    return f"issues/{dest_name}"
