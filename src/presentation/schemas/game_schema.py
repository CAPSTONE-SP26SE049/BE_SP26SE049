from pydantic import BaseModel
from typing import List, Dict, Any, Optional

# --- Shared Base Models ---
class ChallengeBase(BaseModel):
    id: int
    type: str
    content_text: str
    phonetic_transcription_ipa: str
    reference_audio_url: str
    focus_phonemes: Dict[str, Any]

    class Config:
        from_attributes = True

class LevelBase(BaseModel):
    id: int
    level_order: int
    name: str
    description: str
    min_stars_required: int

    class Config:
        from_attributes = True

class RegionBase(BaseModel):
    id: int
    name: str
    description: str

    class Config:
        from_attributes = True

# --- API Responses ---

class LevelSummary(LevelBase):
    pass

class RegionMapResponse(RegionBase):
    """Region data with nested levels (lightweight) for the Map"""
    levels: List[LevelSummary]

class LevelContentResponse(LevelBase):
    """Detailed Level data with list of Challenges"""
    challenges: List[ChallengeBase]
