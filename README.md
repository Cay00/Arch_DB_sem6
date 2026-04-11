# UrbanFix

# Update Zadanie 3
- utworzono bazę PostgreSQL w któej przechowywane są dane kont
- zgłaszanie usterek działa, każde zgłoszenie przypisywane jest do zalogowanego użytkownika, dostępne do podglądu przez webhooki
- każde zgłoszenie ma odpowiedni status rozpatrywania
- Użytkownik Official ma dostęp do wszystkich zgłoszeń, oraz może zmieniać ich status

Projekt do zglaszania usterek miejskich:
- aplikacja Android (Kotlin + Firebase Auth),
- backend FastAPI (Python 3.11),
- PostgreSQL.

## Funkcje

- Rejestracja i logowanie uzytkownika przez Firebase.
- Po rejestracji automatyczny zapis uzytkownika takze do backendu (`POST /users`).
- Ekran Home z kafelkami typow problemow.
- Formularz zglaszania "Uszkodzenie nawierzchni" (zapis do PostgreSQL przez `POST /issues`).
- Automatyczne przypisanie zgloszenia do zalogowanego usera (`user_id`).
- Zakladka **Zgloszenia** pokazuje tylko zgloszenia zalogowanego uzytkownika.

## Wymagania

- Android Studio / VS Code do uruchomienia aplikacji Android
- Python 3.11+
- Docker Desktop (do PostgreSQL)

## 1. Uruchomienie bazy PostgreSQL (Docker)

W terminalu:

```powershell
docker run --name urbanfix-postgres `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=urbanfix `
  -p 5432:5432 -d postgres:16
```

Sprawdzenie:

```powershell
docker ps
Test-NetConnection localhost -Port 5432
```

## 2. Uruchomienie backendu FastAPI

Przejdz do katalogu backend:

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
```

Ustaw URL bazy i uruchom serwer:

```powershell
$env:DATABASE_URL="postgresql+psycopg://postgres:postgres@localhost:5432/urbanfix"
uvicorn app.main:app --reload
```

Backend bedzie dostepny pod:
- `http://127.0.0.1:8000`
- `http://127.0.0.1:8000/docs`

## 3. Uruchomienie aplikacji Android

1. Otworz projekt w Android Studio.
2. Uruchom emulator Android.
3. Run aplikacji.

Uwaga:
- aplikacja komunikuje sie z backendem pod `http://10.0.2.2:8000`,
- backend musi byc uruchomiony lokalnie przed testami rejestracji i zglaszania.

## API (FastAPI — `http://...:8000/`)

### Android (Firebase + Postgres)

- `POST /users` — sync po Firebase: `email`, `password_hash`, `firebase_uid` (z Firebase User.uid). Odpowiedz 201 (nowy) lub 200 (aktualizacja).
- `GET /users/by-email?email=...` — m.in. `id` do `user_id` w zgloszeniu.
- `POST /issues`, `GET /issues?user_id=<id>` — zgloszenia.

### Rejestracja / logowanie tylko w backendzie (JWT)

- `POST /auth/register` — `email`, `password`, `display_name`.
- `POST /auth/login` — `email`, `password`.

### Inne

- `GET /health`, `GET /users`

Przy pierwszym starcie backend robi `create_all`. Po **zmianie modeli** usuń plik `.db` (SQLite) albo odtworz tabele w Postgresie.
