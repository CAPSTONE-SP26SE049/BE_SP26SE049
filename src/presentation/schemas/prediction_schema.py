from pydantic import BaseModel, Field
from typing import Dict, Any, Optional
from datetime import datetime


class PredictionRequest(BaseModel):
    """Schema for prediction request"""
    input_data: Dict[str, Any] = Field(..., description="Input data for prediction")
    user_id: Optional[int] = Field(None, description="Optional user ID")

    class Config:
        json_schema_extra = {
            "example": {
                "input_data": {
                    "feature1": 0.5,
                    "feature2": 1.0,
                    "text": "sample text"
                },
                "user_id": 1
            }
        }


class PredictionResponse(BaseModel):
    """Schema for prediction response"""
    id: int
    prediction_result: Dict[str, Any]
    confidence_score: Optional[float]
    model_version: str
    processing_time_ms: float
    created_at: datetime

    class Config:
        from_attributes = True


class PredictionHistory(BaseModel):
    """Schema for prediction history"""
    id: int
    input_data: Dict[str, Any]
    prediction_result: Dict[str, Any]
    confidence_score: Optional[float]
    created_at: datetime

    class Config:
        from_attributes = True

