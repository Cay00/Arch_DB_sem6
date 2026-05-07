from __future__ import annotations

import argparse
import random
import sys
from pathlib import Path
from datetime import datetime, timedelta, timezone

ROOT_DIR = Path(__file__).resolve().parents[1]
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from app.core.issue_category import ALLOWED_CATEGORIES
from app.core.issue_status import ALL_STATUSES
from app.db.init_db import init_db
from app.db.session import SessionLocal
from app.models.issue import Issue
from app.models.user import User

WROCLAW_LAT_MIN = 51.03
WROCLAW_LAT_MAX = 51.16
WROCLAW_LNG_MIN = 16.86
WROCLAW_LNG_MAX = 17.18

SAMPLE_LOCATIONS = [
    "ul. Legnicka",
    "ul. Grabiszyńska",
    "ul. Powstańców Śląskich",
    "ul. Hallera",
    "ul. Traugutta",
    "ul. Świdnicka",
    "ul. Jedności Narodowej",
    "ul. Oławska",
    "ul. Księcia Witolda",
    "ul. Mińska",
    "ul. Strzegomska",
    "ul. Buforowa",
]

SAMPLE_TITLES = [
    "Uszkodzona nawierzchnia",
    "Zniszczony chodnik",
    "Niebezpieczne przejście",
    "Awaria oświetlenia",
    "Zalegające śmieci",
    "Zdewastowana infrastruktura",
    "Zniszczona zieleń",
    "Nieporządek przy przystanku",
    "Niedokończona inwestycja",
    "Uszkodzona latarnia",
]

SAMPLE_DESCRIPTIONS = [
    "Mieszkańcy zgłaszają, że problem utrudnia codzienne poruszanie się i wymaga interwencji.",
    "Stan techniczny pogarsza się od kilku tygodni, a miejsce jest często uczęszczane.",
    "W godzinach wieczornych widoczność jest ograniczona, co zwiększa ryzyko zdarzeń.",
    "Element infrastruktury został uszkodzony i wymaga naprawy przez służby miejskie.",
    "W okolicy występuje zwiększony ruch pieszy, dlatego sprawa jest pilna.",
]


def _random_point_wroclaw() -> tuple[float, float]:
    lat = round(random.uniform(WROCLAW_LAT_MIN, WROCLAW_LAT_MAX), 6)
    lng = round(random.uniform(WROCLAW_LNG_MIN, WROCLAW_LNG_MAX), 6)
    return lat, lng


def _random_location_label(lat: float, lng: float) -> str:
    street = random.choice(SAMPLE_LOCATIONS)
    number = random.randint(1, 150)
    return f"{street} {number}, Wrocław ({lat}, {lng})"


def _ensure_seed_user(email: str) -> int:
    with SessionLocal() as db:
        existing = db.query(User).filter(User.email == email).first()
        if existing is not None:
            return existing.id
        user = User(
            email=email,
            first_name="Demo",
            last_name="Seeder",
            display_name="Demo Seeder",
            account_type="citizen",
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        return user.id


def seed_issues(count: int, user_email: str) -> None:
    init_db()
    user_id = _ensure_seed_user(user_email)
    categories = list(ALLOWED_CATEGORIES)
    statuses = list(ALL_STATUSES)

    with SessionLocal() as db:
        for _ in range(count):
            lat, lng = _random_point_wroclaw()
            issue = Issue(
                title=random.choice(SAMPLE_TITLES),
                description=random.choice(SAMPLE_DESCRIPTIONS),
                category=random.choice(categories),
                status=random.choice(statuses),
                location=_random_location_label(lat, lng),
                location_lat=lat,
                location_lng=lng,
                user_id=user_id,
                created_at=datetime.now(timezone.utc) - timedelta(days=random.randint(0, 60)),
            )
            db.add(issue)
        db.commit()

    print(f"Dodano {count} przykładowych zgłoszeń dla użytkownika: {user_email}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed przykładowych zgłoszeń dla UrbanFix")
    parser.add_argument("--count", type=int, default=20, help="Liczba rekordów do dodania")
    parser.add_argument(
        "--user-email",
        type=str,
        default="demo.seed@urbanfixdemo.com",
        help="E-mail użytkownika, pod którego dodawane są zgłoszenia",
    )
    args = parser.parse_args()
    if args.count <= 0:
        raise SystemExit("--count musi być > 0")
    seed_issues(args.count, args.user_email.strip())


if __name__ == "__main__":
    main()
