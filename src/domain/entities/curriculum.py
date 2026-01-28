from dataclasses import dataclass, field
from typing import Optional, List, Dict, Any
from enum import Enum
from .user import RegionType

class ChallengeType(str, Enum):
    WORD = "word"
    SENTENCE = "sentence"
    CONVERSATION = "conversation"

@dataclass
class Challenge:
    """Specific exercise content"""
    id: Optional[int]
    level_id: int
    type: ChallengeType
    content_text: str
    phonetic_transcription_ipa: str
    reference_audio_url: str
    focus_phonemes: Dict[str, Any] # e.g. {"n": 1, "l": 1}

@dataclass
class Level:
    """Progression Level"""
    id: Optional[int]
    region_id: int
    level_order: int
    name: str # e.g. "Basic N/L"
    description: str
    min_stars_required: int
    challenges: List[Challenge] = field(default_factory=list)

@dataclass
class Region:
    """Geographical Region"""
    id: Optional[int]
    name: RegionType
    description: str
    levels: List[Level] = field(default_factory=list)
