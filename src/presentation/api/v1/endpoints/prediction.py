from fastapi import APIRouter, Depends, HTTPException, status
from typing import List
from sqlalchemy.orm import Session

from ....schemas.prediction_schema import (
    PredictionRequest,
    PredictionResponse,
    PredictionHistory
)
from .....infrastructure.database.connection import get_db
from .....infrastructure.database.repositories.prediction_repository_impl import PredictionRepositoryImpl
from .....infrastructure.ai.model_loader import ModelLoader
from .....application.services.ai_service import AIService
from .....domain.use_cases.prediction_use_case import (
    PredictUseCase,
    GetPredictionHistoryUseCase
)

router = APIRouter(prefix="/predictions", tags=["Predictions"])


# Dependencies
def get_prediction_repository(db: Session = Depends(get_db)):
    return PredictionRepositoryImpl(db)


def get_ai_service():
    model_loader = ModelLoader()
    return AIService(model_loader)


@router.post("/", response_model=PredictionResponse, status_code=status.HTTP_201_CREATED)
async def create_prediction(
    request: PredictionRequest,
    prediction_repository: PredictionRepositoryImpl = Depends(get_prediction_repository),
    ai_service: AIService = Depends(get_ai_service)
):
    """
    Create a new prediction

    - **input_data**: Input data for the AI model
    - **user_id**: Optional user ID
    """
    try:
        use_case = PredictUseCase(prediction_repository, ai_service)
        result = await use_case.execute(
            input_data=request.input_data,
            user_id=request.user_id
        )

        return PredictionResponse(
            id=result.id,
            prediction_result=result.prediction_result,
            confidence_score=result.confidence_score,
            model_version=result.model_version,
            processing_time_ms=result.processing_time_ms,
            created_at=result.created_at
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction failed: {str(e)}"
        )


@router.get("/user/{user_id}", response_model=List[PredictionHistory])
async def get_user_predictions(
    user_id: int,
    skip: int = 0,
    limit: int = 100,
    prediction_repository: PredictionRepositoryImpl = Depends(get_prediction_repository)
):
    """
    Get prediction history for a specific user

    - **user_id**: User ID
    - **skip**: Number of records to skip (pagination)
    - **limit**: Maximum number of records to return
    """
    try:
        use_case = GetPredictionHistoryUseCase(prediction_repository)
        predictions = await use_case.execute(user_id, skip, limit)

        return [
            PredictionHistory(
                id=pred.id,
                input_data=pred.input_data,
                prediction_result=pred.prediction_result,
                confidence_score=pred.confidence_score,
                created_at=pred.created_at
            )
            for pred in predictions
        ]
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to retrieve predictions: {str(e)}"
        )


@router.get("/{prediction_id}", response_model=PredictionResponse)
async def get_prediction(
    prediction_id: int,
    prediction_repository: PredictionRepositoryImpl = Depends(get_prediction_repository)
):
    """
    Get a specific prediction by ID

    - **prediction_id**: Prediction ID
    """
    prediction = await prediction_repository.get_by_id(prediction_id)

    if not prediction:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Prediction with ID {prediction_id} not found"
        )

    return PredictionResponse(
        id=prediction.id,
        prediction_result=prediction.prediction_result,
        confidence_score=prediction.confidence_score,
        model_version=prediction.model_version,
        processing_time_ms=prediction.processing_time_ms,
        created_at=prediction.created_at
    )

