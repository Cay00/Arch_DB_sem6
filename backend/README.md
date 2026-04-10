# UrbanFix Backend (FastAPI)

Backend API for reporting city issues with JWT authentication and PostgreSQL storage.

## Features
- FastAPI + SQLAlchemy 2.x
- PostgreSQL (psycopg3 driver)
- JWT authentication
- Password hashing with bcrypt

## Setup
1. Create virtualenv and install dependencies:
   - `python -m venv .venv`
   - Windows PowerShell: `.venv\Scripts\Activate.ps1`
   - `pip install -r requirements.txt`
2. Copy `.env.example` to `.env` and update secrets.
3. Run API:
   - `uvicorn app.main:app --reload`

## Endpoints
- `GET /api/v1/health`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/reports` (auth required)
- `GET /api/v1/reports`
