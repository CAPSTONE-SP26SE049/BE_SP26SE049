import pytest
from src.domain.entities.user import UserEntity


def test_user_entity_creation():
    """Test creating a user entity"""
    user = UserEntity(
        username="testuser",
        email="test@example.com",
        full_name="Test User"
    )

    assert user.username == "testuser"
    assert user.email == "test@example.com"
    assert user.full_name == "Test User"
    assert user.is_active is True


def test_user_entity_validation():
    """Test user entity validation"""
    with pytest.raises(ValueError):
        UserEntity(username="", email="test@example.com")

    with pytest.raises(ValueError):
        UserEntity(username="testuser", email="")

