"""
Logging Middleware
Log tất cả các request và response
"""
import time
import logging
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from typing import Callable

# Configure logger
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class LoggingMiddleware(BaseHTTPMiddleware):
    """Middleware for logging all requests and responses"""

    async def dispatch(self, request: Request, call_next: Callable):
        """Log request and response details"""

        # Start timer
        start_time = time.time()

        # Log request
        logger.info(f"➡️  {request.method} {request.url.path}")
        logger.debug(f"Headers: {dict(request.headers)}")

        # Get client info
        client_host = request.client.host if request.client else "unknown"

        # Process request
        try:
            response = await call_next(request)

            # Calculate processing time
            process_time = time.time() - start_time

            # Log response
            logger.info(
                f"⬅️  {request.method} {request.url.path} "
                f"- Status: {response.status_code} "
                f"- Time: {process_time:.3f}s "
                f"- Client: {client_host}"
            )

            # Add custom headers
            response.headers["X-Process-Time"] = str(process_time)

            return response

        except Exception as e:
            process_time = time.time() - start_time
            logger.error(
                f"❌ {request.method} {request.url.path} "
                f"- Error: {str(e)} "
                f"- Time: {process_time:.3f}s "
                f"- Client: {client_host}"
            )
            raise

