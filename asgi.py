"""
Punkt wejścia ASGI z katalogu głównego repozytorium.

Uruchomienie:
  uvicorn asgi:app --reload --reload-dir backend

Pakiet `app` leży w ./backend — bez tego pliku uvicorn z korzenia nie znajdzie `app.main`.
"""

from __future__ import annotations

import sys
from pathlib import Path

_backend = Path(__file__).resolve().parent / "backend"
sys.path.insert(0, str(_backend))

from app.main import app as app  # noqa: E402

__all__ = ["app"]
