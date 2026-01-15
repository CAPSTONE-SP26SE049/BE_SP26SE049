import pytest
    return TestClient(app)

    from src.presentation.main import app
    from fastapi.testclient import TestClient
    """Create a test client"""
def client():
@pytest.fixture


        Base.metadata.drop_all(bind=engine)
        db.close()
    finally:
        yield db
    try:
    db = TestingSessionLocal()
    Base.metadata.create_all(bind=engine)
    """Create a fresh database for each test"""
def db():
@pytest.fixture(scope="function")


TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
)
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
engine = create_engine(

SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
# Test database

from src.infrastructure.database.connection import Base
from sqlalchemy.orm import sessionmaker
from sqlalchemy import create_engine

