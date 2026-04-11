# UrbanFix — backend (FastAPI + PostgreSQL)

Prosty API: użytkownicy (Firebase + opcjonalnie hasło w Postgres), JWT z `/auth`, zgłoszenia `issues`.

## Uruchomienie

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
# ustaw DATABASE_URL (Postgres) i JWT_SECRET_KEY
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Przy starcie wywoływane jest `create_all` — **przy zmianie schematu** usuń bazę / zrób migrację ręcznie.

## Endpointy (wszystko w korzeniu, bez `/api/v1`)

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| GET | `/health` | OK |
| POST | `/auth/register` | Konto tylko w Postgres + JWT (`email`, `password`, `display_name?`) |
| POST | `/auth/login` | JWT (`email`, `password`) — wymaga `hashed_password` w bazie |
| POST | `/users` | Sync z aplikacji Firebase: `email`, `firebase_uid?`, `password_hash?` (plain → bcrypt). 201 / 200 / 409 |
| GET | `/users` | Lista |
| GET | `/users/by-email?email=` | Po `id` pod zgłoszenia |
| POST | `/issues` | Zgłoszenie (`user_id`, `title`, `description`, `category`, `location`, `status?`) |
| GET | `/issues` | Lista: `?user_id=` (mieszkaniec), `?official_email=` (Official — wszystkie), opcjonalnie `?issues_list_secret=` gdy w `.env` jest `ISSUES_LIST_SECRET` (webhook / integracje). Przy domyślnym `ENV=development` samo `GET /issues` zwraca pełną listę (wygoda `/docs`); przy `ENV=production` bez parametrów — 422. |

OpenAPI: `/docs`

## Model

- **users** — `email`, `firebase_uid` (unikalne, opcjonalne), `hashed_password` (opcjonalne), `display_name`, `created_at`
- **issues** — powiązane z `users.id`
