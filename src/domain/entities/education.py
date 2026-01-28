from dataclasses import dataclass
from typing import Optional
from datetime import datetime

@dataclass
class ClassMember:
    """Link between Student and Classroom"""
    classroom_id: int
    student_id: int
    joined_at: datetime

@dataclass
class Classroom:
    """Educator's Class Group"""
    id: Optional[int]
    educator_id: int
    name: str
    code: str
    # Members list can be loaded via repo if needed
