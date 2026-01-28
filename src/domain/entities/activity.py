from dataclasses import dataclass
from typing import Optional, Dict, Any
from datetime import datetime

@dataclass
class Attempt:
    """User Practice Attempt"""
    id: Optional[int]
    user_id: int
    challenge_id: int
    audio_url: str
    score: int
    ai_analysis_json: Dict[str, Any]
    is_passed: bool
    created_at: Optional[datetime] = None
