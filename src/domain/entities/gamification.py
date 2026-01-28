from dataclasses import dataclass
from typing import Optional, Dict, Any
from datetime import datetime

@dataclass
class Badge:
    """Achievement Badge"""
    id: Optional[int]
    name: str
    icon_url: str
    criteria_json: Dict[str, Any]

@dataclass
class UserBadge:
    """Badge earned by User"""
    user_id: int
    badge_id: int
    earned_at: datetime
