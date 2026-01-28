from dataclasses import dataclass, field
from typing import Optional
from datetime import datetime
from enum import Enum

class UserRole(str, Enum):
    PLAYER = "player"
    EDUCATOR = "educator"
    ADMIN = "admin"

class RegionType(str, Enum):
    NORTH = "north"
    CENTRAL = "central"
    SOUTH = "south"

@dataclass
class User:
    """Core User Entity"""
    id: Optional[int]
    username: str
    email: str
    role: UserRole
    password_hash: str # In domain, we might keep this or hide it. Keeping for now.
    is_active: bool = True
    created_at: Optional[datetime] = None
    
    # Relationships could be loaded here or handled via separate calls. 
    # For Clean Architecture entities, we often keep them lightweight.

@dataclass
class UserProfile:
    """Extended Player Profile"""
    id: Optional[int]
    user_id: int
    full_name: Optional[str] = None
    avatar_url: Optional[str] = None
    
    # Gamification
    total_stars: int = 0
    current_streak: int = 0
    total_xp: int = 0
    
    # Regional Focus
    native_region: Optional[RegionType] = None
    target_region: Optional[RegionType] = None
