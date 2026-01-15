"""
Rate Limit Middleware
Giới hạn số lượng request từ một IP trong khoảng thời gian
"""
import time
from fastapi import Request, HTTPException, status
from starlette.middleware.base import BaseHTTPMiddleware
from typing import Dict, Tuple
from collections import defaultdict
from src.infrastructure.config.settings import settings


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Middleware for rate limiting requests by IP address"""

    def __init__(self, app, requests: int = None, period: int = None):
        super().__init__(app)
        self.requests = requests or settings.RATE_LIMIT_REQUESTS
        self.period = period or settings.RATE_LIMIT_PERIOD

        # Store: {ip_address: [(timestamp1, timestamp2, ...)]}
        self.request_counts: Dict[str, list] = defaultdict(list)

        # Excluded paths (không giới hạn rate)
        self.excluded_paths = [
            "/",
            "/docs",
            "/redoc",
            "/openapi.json",
            "/api/v1/health"
        ]

    async def dispatch(self, request: Request, call_next):
        """Check rate limit before processing request"""

        # Skip rate limiting for excluded paths
        if any(request.url.path.startswith(path) for path in self.excluded_paths):
            return await call_next(request)

        # Get client IP
        client_ip = request.client.host if request.client else "unknown"

        # Get current timestamp
        current_time = time.time()

        # Clean old timestamps (outside the time window)
        self.request_counts[client_ip] = [
            timestamp for timestamp in self.request_counts[client_ip]
            if current_time - timestamp < self.period
        ]

        # Check if rate limit exceeded
        if len(self.request_counts[client_ip]) >= self.requests:
            # Calculate retry after time
            oldest_timestamp = self.request_counts[client_ip][0]
            retry_after = int(self.period - (current_time - oldest_timestamp)) + 1

            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Rate limit exceeded. Try again in {retry_after} seconds.",
                headers={"Retry-After": str(retry_after)}
            )

        # Add current request timestamp
        self.request_counts[client_ip].append(current_time)

        # Process request
        response = await call_next(request)

        # Add rate limit headers
        remaining = self.requests - len(self.request_counts[client_ip])
        response.headers["X-RateLimit-Limit"] = str(self.requests)
        response.headers["X-RateLimit-Remaining"] = str(max(0, remaining))
        response.headers["X-RateLimit-Reset"] = str(int(current_time + self.period))

        return response

    def reset_limits(self):
        """Reset all rate limits (useful for testing)"""
        self.request_counts.clear()

