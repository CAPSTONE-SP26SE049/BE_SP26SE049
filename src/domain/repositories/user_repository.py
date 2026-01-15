from abc import ABC, abstractmethod
from typing import Optional, List
from ..entities.user import UserEntity


class UserRepository(ABC):
    """User repository interface"""

    @abstractmethod
    async def create(self, user: UserEntity) -> UserEntity:
        """Create a new user"""
        pass

    @abstractmethod
    async def get_by_id(self, user_id: int) -> Optional[UserEntity]:
        """Get user by ID"""
        pass

    @abstractmethod
    async def get_by_email(self, email: str) -> Optional[UserEntity]:
        """Get user by email"""
        pass

    @abstractmethod
    async def get_all(self, skip: int = 0, limit: int = 100) -> List[UserEntity]:
        """Get all users with pagination"""
        pass

    @abstractmethod
    async def update(self, user: UserEntity) -> UserEntity:
        """Update user"""
        pass

    @abstractmethod
    async def delete(self, user_id: int) -> bool:
        """Delete user"""
        pass

