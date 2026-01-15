from dataclasses import dataclass
from typing import Optional
from datetime import datetime


@dataclass
class UserRequestDTO:
    """DTO for user request"""
    username: str
    email: str
    full_name: Optional[str] = None


@dataclass
class UserResponseDTO:
    """DTO for user response"""
    id: int
    username: str
    email: str
    full_name: Optional[str]
    is_active: bool
    created_at: datetime

