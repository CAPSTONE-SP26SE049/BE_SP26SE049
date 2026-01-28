import pytest
from src.domain.entities.user import User, UserRole

def test_user_creation():
    """Test creating a user entity"""
    user = User(
        id=1,
        username="testuser",
        email="test@example.com",
        role=UserRole.PLAYER,
        password_hash="hashed_secret"
    )

    assert user.username == "testuser"
    assert user.email == "test@example.com"
    assert user.role == UserRole.PLAYER
    assert user.is_active is True

def test_user_role_enum():
    """Test UserRole enum values"""
    assert UserRole.PLAYER == "player"
    assert UserRole.ADMIN == "admin"
