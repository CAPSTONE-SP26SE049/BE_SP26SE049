from typing import Optional, List
from ..entities.user import UserEntity
from ..repositories.user_repository import UserRepository


class CreateUserUseCase:
    """Use case for creating a user"""

    def __init__(self, user_repository: UserRepository):
        self.user_repository = user_repository

    async def execute(self, username: str, email: str, full_name: str = None) -> UserEntity:
        """Create a new user"""
        # Check if user already exists
        existing_user = await self.user_repository.get_by_email(email)
        if existing_user:
            raise ValueError(f"User with email {email} already exists")

        # Create user entity
        user = UserEntity(
            username=username,
            email=email,
            full_name=full_name,
            is_active=True
        )

        # Save to repository
        return await self.user_repository.create(user)


class GetUserUseCase:
    """Use case for getting a user"""

    def __init__(self, user_repository: UserRepository):
        self.user_repository = user_repository

    async def execute(self, user_id: int) -> Optional[UserEntity]:
        """Get user by ID"""
        user = await self.user_repository.get_by_id(user_id)
        if not user:
            raise ValueError(f"User with ID {user_id} not found")
        return user


class GetAllUsersUseCase:
    """Use case for getting all users"""

    def __init__(self, user_repository: UserRepository):
        self.user_repository = user_repository

    async def execute(self, skip: int = 0, limit: int = 100) -> List[UserEntity]:
        """Get all users with pagination"""
        return await self.user_repository.get_all(skip, limit)

