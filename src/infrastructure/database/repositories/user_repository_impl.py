from typing import Optional, List
from sqlalchemy.orm import Session
from ....domain.entities.user import UserEntity
from ....domain.repositories.user_repository import UserRepository
from ..models import UserModel


class UserRepositoryImpl(UserRepository):
    """User repository implementation using SQLAlchemy"""

    def __init__(self, db: Session):
        self.db = db

    def _to_entity(self, model: UserModel) -> UserEntity:
        """Convert database model to domain entity"""
        return UserEntity(
            id=model.id,
            username=model.username,
            email=model.email,
            full_name=model.full_name,
            is_active=model.is_active,
            created_at=model.created_at,
            updated_at=model.updated_at
        )

    def _to_model(self, entity: UserEntity) -> UserModel:
        """Convert domain entity to database model"""
        return UserModel(
            id=entity.id,
            username=entity.username,
            email=entity.email,
            full_name=entity.full_name,
            is_active=entity.is_active
        )

    async def create(self, user: UserEntity) -> UserEntity:
        """Create a new user"""
        db_user = self._to_model(user)
        self.db.add(db_user)
        self.db.commit()
        self.db.refresh(db_user)
        return self._to_entity(db_user)

    async def get_by_id(self, user_id: int) -> Optional[UserEntity]:
        """Get user by ID"""
        db_user = self.db.query(UserModel).filter(UserModel.id == user_id).first()
        return self._to_entity(db_user) if db_user else None

    async def get_by_email(self, email: str) -> Optional[UserEntity]:
        """Get user by email"""
        db_user = self.db.query(UserModel).filter(UserModel.email == email).first()
        return self._to_entity(db_user) if db_user else None

    async def get_all(self, skip: int = 0, limit: int = 100) -> List[UserEntity]:
        """Get all users with pagination"""
        db_users = self.db.query(UserModel).offset(skip).limit(limit).all()
        return [self._to_entity(user) for user in db_users]

    async def update(self, user: UserEntity) -> UserEntity:
        """Update user"""
        db_user = self.db.query(UserModel).filter(UserModel.id == user.id).first()
        if db_user:
            db_user.username = user.username
            db_user.email = user.email
            db_user.full_name = user.full_name
            db_user.is_active = user.is_active
            self.db.commit()
            self.db.refresh(db_user)
            return self._to_entity(db_user)
        return None

    async def delete(self, user_id: int) -> bool:
        """Delete user"""
        db_user = self.db.query(UserModel).filter(UserModel.id == user_id).first()
        if db_user:
            self.db.delete(db_user)
            self.db.commit()
            return True
        return False

