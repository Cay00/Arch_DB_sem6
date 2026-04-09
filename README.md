# UrbanFix

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

## API - najwazniejsze endpointy

- `GET /health` - status API.
- `POST /users` - zapis usera do backendu.
- `GET /users` - lista userow.
- `GET /users/by-email?email=...` - user po emailu (uzywane przez aplikacje).
- `POST /issues` - utworzenie zgloszenia.
- `GET /issues` - lista zgloszen.
- `GET /issues?user_id=<id>` - lista zgloszen konkretnego usera.
