from fastapi import APIRouter

from app.api.routes.health import router as health_router
from app.api.routes.issues import router as issues_router
from app.api.routes.users import router as users_router

api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(users_router)
api_router.include_router(issues_router)
