from dataclasses import dataclass
from typing import Optional, Dict, Any
from datetime import datetime


@dataclass
class PredictionEntity:
    """Prediction domain entity"""
    id: Optional[int] = None
    user_id: Optional[int] = None
    input_data: Dict[str, Any] = None
    prediction_result: Optional[Dict[str, Any]] = None
    model_version: str = "v1.0"
    confidence_score: Optional[float] = None
    processing_time_ms: Optional[float] = None
    created_at: Optional[datetime] = None

    def __post_init__(self):
        if self.input_data is None:
            raise ValueError("Input data is required")
        if self.confidence_score is not None:
            if not 0 <= self.confidence_score <= 1:
                raise ValueError("Confidence score must be between 0 and 1")


@dataclass
class ModelEntity:
    """AI Model domain entity"""
    id: Optional[int] = None
    name: str = ""
    version: str = "v1.0"
    path: str = ""
    model_type: str = "classification"
    is_active: bool = True
    metadata: Optional[Dict[str, Any]] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    def __post_init__(self):
        if not self.name:
            raise ValueError("Model name is required")
        if not self.path:
            raise ValueError("Model path is required")

