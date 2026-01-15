from fastapi import APIRouter, Depends, HTTPException, status
from typing import List
from sqlalchemy.orm import Session

from ....schemas.user_schema import UserCreate, UserResponse, UserUpdate
from .....infrastructure.database.connection import get_db
from .....infrastructure.database.repositories.user_repository_impl import UserRepositoryImpl
from .....domain.use_cases.user_use_case import (
    CreateUserUseCase,
    GetUserUseCase,
    GetAllUsersUseCase
)

router = APIRouter(prefix="/users", tags=["Users"])


# Dependency
def get_user_repository(db: Session = Depends(get_db)):
    return UserRepositoryImpl(db)


@router.post("/", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def create_user(
    user: UserCreate,
    user_repository: UserRepositoryImpl = Depends(get_user_repository)
):
    """
    Create a new user

    - **username**: Unique username (3-50 characters)
    - **email**: Valid email address
    - **full_name**: Optional full name
    """
    try:
        use_case = CreateUserUseCase(user_repository)
        result = await use_case.execute(
            username=user.username,
            email=user.email,
            full_name=user.full_name
        )

        return UserResponse(
            id=result.id,
            username=result.username,
            email=result.email,
            full_name=result.full_name,
            is_active=result.is_active,
            created_at=result.created_at
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create user: {str(e)}"
        )


@router.get("/{user_id}", response_model=UserResponse)
async def get_user(
    user_id: int,
    user_repository: UserRepositoryImpl = Depends(get_user_repository)
):
    """
    Get a user by ID

    - **user_id**: User ID
    """
    try:
        use_case = GetUserUseCase(user_repository)
        result = await use_case.execute(user_id)

        return UserResponse(
            id=result.id,
            username=result.username,
            email=result.email,
            full_name=result.full_name,
            is_active=result.is_active,
            created_at=result.created_at
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to retrieve user: {str(e)}"
        )


@router.get("/", response_model=List[UserResponse])
async def get_all_users(
    skip: int = 0,
    limit: int = 100,
    user_repository: UserRepositoryImpl = Depends(get_user_repository)
):
    """
    Get all users with pagination

    - **skip**: Number of records to skip (pagination)
    - **limit**: Maximum number of records to return
    """
    try:
        use_case = GetAllUsersUseCase(user_repository)
        results = await use_case.execute(skip, limit)

        return [
            UserResponse(
                id=user.id,
                username=user.username,
                email=user.email,
                full_name=user.full_name,
                is_active=user.is_active,
                created_at=user.created_at
            )
            for user in results
        ]
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to retrieve users: {str(e)}"
        )

