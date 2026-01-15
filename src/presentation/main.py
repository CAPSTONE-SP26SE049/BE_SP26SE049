from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from .api.router import api_router
from src.infrastructure.config.settings import settings
from src.infrastructure.database.connection import init_db
from .middleware import (
    LoggingMiddleware,
    ErrorHandlerMiddleware,
    RateLimitMiddleware,
    # AuthMiddleware  # Uncomment để bật authentication
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for startup and shutdown events"""
    # Startup
    print("🚀 Starting AI Backend Service...")
    print(f"📝 API Documentation: http://{settings.HOST}:{settings.PORT}/docs")
    init_db()
    print("✅ Database initialized")

    yield

    # Shutdown
    print("👋 Shutting down AI Backend Service...")


# Create FastAPI app
app = FastAPI(
    title=settings.APP_NAME,
    description="AI Backend Service built with Clean Architecture",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=settings.CORS_CREDENTIALS,
    allow_methods=settings.CORS_METHODS,
    allow_headers=settings.CORS_HEADERS,
)

# Add custom middlewares (thứ tự quan trọng - từ ngoài vào trong)
# 1. Error Handler - xử lý lỗi đầu tiên
app.add_middleware(ErrorHandlerMiddleware)

# 2. Logging - log tất cả request/response
app.add_middleware(LoggingMiddleware)

# 3. Rate Limiting - giới hạn số request
app.add_middleware(RateLimitMiddleware)

# 4. Authentication - kiểm tra JWT token (uncomment để bật)
# app.add_middleware(AuthMiddleware)

# Include API router
app.include_router(api_router, prefix=settings.API_V1_PREFIX)



@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Welcome to AI Backend Service",
        "version": "1.0.0",
        "docs": "/docs",
        "health": f"{settings.API_V1_PREFIX}/health"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )

