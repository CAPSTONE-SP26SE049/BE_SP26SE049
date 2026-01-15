from typing import Dict, Any
import time
from datetime import datetime
from ..entities.model import PredictionEntity
from ..repositories.prediction_repository import PredictionRepository


class PredictUseCase:
    """Use case for making predictions"""

    def __init__(
        self,
        prediction_repository: PredictionRepository,
        ai_service,
    ):
        self.prediction_repository = prediction_repository
        self.ai_service = ai_service

    async def execute(
        self,
        input_data: Dict[str, Any],
        user_id: int = None
    ) -> PredictionEntity:
        """
        Execute prediction use case

        Args:
            input_data: Input data for prediction
            user_id: Optional user ID

        Returns:
            PredictionEntity with prediction results
        """
        start_time = time.time()

        # Perform prediction using AI service
        prediction_result = await self.ai_service.predict(input_data)

        # Calculate processing time
        processing_time = (time.time() - start_time) * 1000  # Convert to ms

        # Create prediction entity
        prediction_entity = PredictionEntity(
            user_id=user_id,
            input_data=input_data,
            prediction_result=prediction_result.get("result"),
            model_version=prediction_result.get("model_version", "v1.0"),
            confidence_score=prediction_result.get("confidence"),
            processing_time_ms=processing_time,
            created_at=datetime.now()
        )

        # Save to repository
        saved_prediction = await self.prediction_repository.create(prediction_entity)

        return saved_prediction


class GetPredictionHistoryUseCase:
    """Use case for getting prediction history"""

    def __init__(self, prediction_repository: PredictionRepository):
        self.prediction_repository = prediction_repository

    async def execute(self, user_id: int, skip: int = 0, limit: int = 100):
        """Get prediction history for a user"""
        return await self.prediction_repository.get_by_user(user_id, skip, limit)

