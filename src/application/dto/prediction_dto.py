from dataclasses import dataclass
from typing import Dict, Any, Optional
from datetime import datetime


@dataclass
class PredictionRequestDTO:
    """DTO for prediction request"""
    input_data: Dict[str, Any]
    user_id: Optional[int] = None


@dataclass
class PredictionResponseDTO:
    """DTO for prediction response"""
    id: int
    prediction_result: Dict[str, Any]
    confidence_score: Optional[float]
    model_version: str
    processing_time_ms: float
    created_at: datetime


@dataclass
class PredictionHistoryDTO:
    """DTO for prediction history"""
    id: int
    input_data: Dict[str, Any]
    prediction_result: Dict[str, Any]
    confidence_score: Optional[float]
    created_at: datetime

