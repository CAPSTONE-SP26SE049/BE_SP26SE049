from fastapi import APIRouter
from datetime import datetime

router = APIRouter(tags=["Health"])


@router.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "AI Backend Service"
    }


@router.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Welcome to AI Backend Service",
        "version": "1.0.0",
        "docs": "/docs"
    }

