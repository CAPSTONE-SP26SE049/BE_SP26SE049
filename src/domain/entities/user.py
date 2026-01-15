from dataclasses import dataclass
from typing import Optional
from datetime import datetime


@dataclass
class UserEntity:
    """User domain entity"""
    id: Optional[int] = None
    username: str = ""
    email: str = ""
    full_name: Optional[str] = None
    is_active: bool = True
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    def __post_init__(self):
        if not self.username:
            raise ValueError("Username is required")
        if not self.email:
            raise ValueError("Email is required")

