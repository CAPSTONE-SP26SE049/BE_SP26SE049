from fastapi import APIRouter
from .v1.endpoints import health, prediction, users

api_router = APIRouter()

# Include all endpoint routers
api_router.include_router(health.router)
api_router.include_router(prediction.router)
api_router.include_router(users.router)

