"""
Error Handler Middleware
Xử lý tất cả các exception và trả về response chuẩn
"""
import logging
from fastapi import Request, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
from typing import Callable

logger = logging.getLogger(__name__)


class ErrorHandlerMiddleware(BaseHTTPMiddleware):
    """Middleware for global error handling"""

    async def dispatch(self, request: Request, call_next: Callable):
        """Catch and handle all exceptions"""

        try:
            response = await call_next(request)
            return response

        except ValueError as e:
            logger.error(f"ValueError: {str(e)}")
            return JSONResponse(
                status_code=status.HTTP_400_BAD_REQUEST,
                content={
                    "success": False,
                    "error": {
                        "code": "VALIDATION_ERROR",
                        "message": str(e)
                    }
                }
            )

        except PermissionError as e:
            logger.error(f"PermissionError: {str(e)}")
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={
                    "success": False,
                    "error": {
                        "code": "PERMISSION_DENIED",
                        "message": str(e)
                    }
                }
            )

        except FileNotFoundError as e:
            logger.error(f"FileNotFoundError: {str(e)}")
            return JSONResponse(
                status_code=status.HTTP_404_NOT_FOUND,
                content={
                    "success": False,
                    "error": {
                        "code": "NOT_FOUND",
                        "message": str(e)
                    }
                }
            )

        except Exception as e:
            logger.exception(f"Unhandled exception: {str(e)}")
            return JSONResponse(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                content={
                    "success": False,
                    "error": {
                        "code": "INTERNAL_SERVER_ERROR",
                        "message": "An unexpected error occurred. Please try again later."
                    }
                }
            )

