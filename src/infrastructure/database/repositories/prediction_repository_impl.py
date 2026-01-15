from typing import Optional, List
from sqlalchemy.orm import Session
from ....domain.entities.model import PredictionEntity, ModelEntity
from ....domain.repositories.prediction_repository import PredictionRepository, ModelRepository
from ..models import PredictionModel, ModelModel


class PredictionRepositoryImpl(PredictionRepository):
    """Prediction repository implementation using SQLAlchemy"""

    def __init__(self, db: Session):
        self.db = db

    def _to_entity(self, model: PredictionModel) -> PredictionEntity:
        """Convert database model to domain entity"""
        return PredictionEntity(
            id=model.id,
            user_id=model.user_id,
            input_data=model.input_data,
            prediction_result=model.prediction_result,
            model_version=model.model_version,
            confidence_score=model.confidence_score,
            processing_time_ms=model.processing_time_ms,
            created_at=model.created_at
        )

    def _to_model(self, entity: PredictionEntity) -> PredictionModel:
        """Convert domain entity to database model"""
        return PredictionModel(
            id=entity.id,
            user_id=entity.user_id,
            input_data=entity.input_data,
            prediction_result=entity.prediction_result,
            model_version=entity.model_version,
            confidence_score=entity.confidence_score,
            processing_time_ms=entity.processing_time_ms
        )

    async def create(self, prediction: PredictionEntity) -> PredictionEntity:
        """Create a new prediction"""
        db_prediction = self._to_model(prediction)
        self.db.add(db_prediction)
        self.db.commit()
        self.db.refresh(db_prediction)
        return self._to_entity(db_prediction)

    async def get_by_id(self, prediction_id: int) -> Optional[PredictionEntity]:
        """Get prediction by ID"""
        db_prediction = self.db.query(PredictionModel).filter(PredictionModel.id == prediction_id).first()
        return self._to_entity(db_prediction) if db_prediction else None

    async def get_by_user(self, user_id: int, skip: int = 0, limit: int = 100) -> List[PredictionEntity]:
        """Get predictions by user ID"""
        db_predictions = (
            self.db.query(PredictionModel)
            .filter(PredictionModel.user_id == user_id)
            .offset(skip)
            .limit(limit)
            .all()
        )
        return [self._to_entity(pred) for pred in db_predictions]

    async def get_all(self, skip: int = 0, limit: int = 100) -> List[PredictionEntity]:
        """Get all predictions with pagination"""
        db_predictions = self.db.query(PredictionModel).offset(skip).limit(limit).all()
        return [self._to_entity(pred) for pred in db_predictions]


class ModelRepositoryImpl(ModelRepository):
    """Model repository implementation using SQLAlchemy"""

    def __init__(self, db: Session):
        self.db = db

    def _to_entity(self, model: ModelModel) -> ModelEntity:
        """Convert database model to domain entity"""
        return ModelEntity(
            id=model.id,
            name=model.name,
            version=model.version,
            path=model.path,
            model_type=model.model_type,
            is_active=model.is_active,
            metadata=model.metadata_info,
            created_at=model.created_at,
            updated_at=model.updated_at
        )

    def _to_model(self, entity: ModelEntity) -> ModelModel:
        """Convert domain entity to database model"""
        return ModelModel(
            id=entity.id,
            name=entity.name,
            version=entity.version,
            path=entity.path,
            model_type=entity.model_type,
            is_active=entity.is_active,
            metadata_info=entity.metadata
        )

    async def create(self, model: ModelEntity) -> ModelEntity:
        """Create a new model"""
        db_model = self._to_model(model)
        self.db.add(db_model)
        self.db.commit()
        self.db.refresh(db_model)
        return self._to_entity(db_model)

    async def get_by_id(self, model_id: int) -> Optional[ModelEntity]:
        """Get model by ID"""
        db_model = self.db.query(ModelModel).filter(ModelModel.id == model_id).first()
        return self._to_entity(db_model) if db_model else None

    async def get_active_model(self) -> Optional[ModelEntity]:
        """Get currently active model"""
        db_model = self.db.query(ModelModel).filter(ModelModel.is_active == True).first()
        return self._to_entity(db_model) if db_model else None

    async def get_all(self, skip: int = 0, limit: int = 100) -> List[ModelEntity]:
        """Get all models with pagination"""
        db_models = self.db.query(ModelModel).offset(skip).limit(limit).all()
        return [self._to_entity(model) for model in db_models]

    async def update(self, model: ModelEntity) -> ModelEntity:
        """Update model"""
        db_model = self.db.query(ModelModel).filter(ModelModel.id == model.id).first()
        if db_model:
            db_model.name = model.name
            db_model.version = model.version
            db_model.path = model.path
            db_model.model_type = model.model_type
            db_model.is_active = model.is_active
            db_model.metadata_info = model.metadata
            self.db.commit()
            self.db.refresh(db_model)
            return self._to_entity(db_model)
        return None

