from abc import ABC, abstractmethod
from typing import Optional, List
from ..entities.model import PredictionEntity, ModelEntity


class PredictionRepository(ABC):
    """Prediction repository interface"""

    @abstractmethod
    async def create(self, prediction: PredictionEntity) -> PredictionEntity:
        """Create a new prediction"""
        pass

    @abstractmethod
    async def get_by_id(self, prediction_id: int) -> Optional[PredictionEntity]:
        """Get prediction by ID"""
        pass

    @abstractmethod
    async def get_by_user(self, user_id: int, skip: int = 0, limit: int = 100) -> List[PredictionEntity]:
        """Get predictions by user ID"""
        pass

    @abstractmethod
    async def get_all(self, skip: int = 0, limit: int = 100) -> List[PredictionEntity]:
        """Get all predictions with pagination"""
        pass


class ModelRepository(ABC):
    """Model repository interface"""

    @abstractmethod
    async def create(self, model: ModelEntity) -> ModelEntity:
        """Create a new model"""
        pass

    @abstractmethod
    async def get_by_id(self, model_id: int) -> Optional[ModelEntity]:
        """Get model by ID"""
        pass

    @abstractmethod
    async def get_active_model(self) -> Optional[ModelEntity]:
        """Get currently active model"""
        pass

    @abstractmethod
    async def get_all(self, skip: int = 0, limit: int = 100) -> List[ModelEntity]:
        """Get all models with pagination"""
        pass

    @abstractmethod
    async def update(self, model: ModelEntity) -> ModelEntity:
        """Update model"""
        pass

