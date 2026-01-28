from typing import Generic, TypeVar, Optional, Any, Dict, List
from pydantic import BaseModel, Field
from dataclasses import field
from datetime import datetime

T = TypeVar("T")

class BaseResponse(BaseModel, Generic[T]):
    """
    Standard Wrapper for all API responses.
    """
    success: bool = True
    message: str = "Success"
    data: Optional[T] = None
    meta: Optional[Dict[str, Any]] = None # For pagination (page, total, etc.)
    errors: Optional[List[Dict[str, Any]]] = None # For validation errors
    timestamp: datetime = field(default_factory=datetime.utcnow)

    class Config:
        from_attributes = True

