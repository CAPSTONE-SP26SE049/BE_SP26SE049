"""
Database Models
SQLAlchemy models mapping to database tables
"""
from sqlalchemy import Column, Integer, String, Boolean, DateTime, Float, ForeignKey, JSON
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from .connection import Base


class UserModel(Base):
    """User database model"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    full_name = Column(String, nullable=True)
    hashed_password = Column(String, nullable=True) # Adding password support
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationships
    predictions = relationship("PredictionModel", back_populates="user")


class ModelModel(Base):
    """AI Model database model"""
    __tablename__ = "models"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True, nullable=False)
    version = Column(String, default="v1.0")
    path = Column(String, nullable=False)
    model_type = Column(String, default="classification")
    is_active = Column(Boolean, default=True)
    metadata_info = Column(JSON, nullable=True, name="metadata") # metadata is reserved in SQLAlchemy sometimes, but using name="metadata" or just "metadata" if safe
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())


class PredictionModel(Base):
    """Prediction database model"""
    __tablename__ = "predictions"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    input_data = Column(JSON, nullable=False)
    prediction_result = Column(JSON, nullable=True)
    model_version = Column(String, default="v1.0")
    confidence_score = Column(Float, nullable=True)
    processing_time_ms = Column(Float, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    user = relationship("UserModel", back_populates="predictions")
