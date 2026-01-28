"""
Database Models
SQLAlchemy 2.0 models mapping to database tables for SpeakVN Journey
"""
from enum import Enum
from datetime import datetime
from typing import List, Optional
from sqlalchemy import ForeignKey, String, Integer, Float, Text, Boolean, DateTime, JSON
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func
from .connection import Base

# --- Enums for Static Choices ---
class UserRole(str, Enum):
    PLAYER = "player"
    EDUCATOR = "educator"
    ADMIN = "admin"

class RegionType(str, Enum):
    NORTH = "north"
    CENTRAL = "central"
    SOUTH = "south"

class ChallengeType(str, Enum):
    WORD = "word"
    SENTENCE = "sentence"
    CONVERSATION = "conversation"


# --- Core Identity Models ---

class User(Base):
    __tablename__ = "users"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    email: Mapped[str] = mapped_column(String(100), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    role: Mapped[UserRole] = mapped_column(default=UserRole.PLAYER)
    
    # Password Reset OTP
    reset_code: Mapped[Optional[str]] = mapped_column(String(6), nullable=True) # 6-digit OTP
    reset_expires_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)

    
    # Use server_default for DB-level timestamp
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    is_active: Mapped[bool] = mapped_column(default=True)

    # Relationships
    profile: Mapped["UserProfile"] = relationship(back_populates="user", uselist=False, cascade="all, delete-orphan")
    attempts: Mapped[List["Attempt"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    class_memberships: Mapped[List["ClassMember"]] = relationship(back_populates="student", cascade="all, delete-orphan")


class UserProfile(Base):
    """Extended profile for Players to track Gamification & Stats"""
    __tablename__ = "user_profiles"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True)
    full_name: Mapped[Optional[str]] = mapped_column(String(100))
    avatar_url: Mapped[Optional[str]] = mapped_column(String(255))
    
    # Gamification Stats
    total_stars: Mapped[int] = mapped_column(default=0)
    current_streak: Mapped[int] = mapped_column(default=0)
    total_xp: Mapped[int] = mapped_column(default=0)
    
    # Regional Focus identified by AI Assessment
    native_region: Mapped[Optional[RegionType]] = mapped_column(nullable=True)
    target_region: Mapped[Optional[RegionType]] = mapped_column(nullable=True)

    user: Mapped["User"] = relationship(back_populates="profile")


# --- Content & Curriculum Models ---

class Region(Base):
    """Represents the 3 main regions (North, Central, South)"""
    __tablename__ = "regions"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    name: Mapped[RegionType] = mapped_column(unique=True)
    description: Mapped[str] = mapped_column(Text)
    
    levels: Mapped[List["Level"]] = relationship(back_populates="region", cascade="all, delete-orphan")


class Level(Base):
    """Progression Levels (e.g., Level 1: N/L Confusion)"""
    __tablename__ = "levels"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    region_id: Mapped[int] = mapped_column(ForeignKey("regions.id"))
    level_order: Mapped[int] = mapped_column() # 1, 2, 3...
    name: Mapped[str] = mapped_column(String(100)) # e.g. "Basic N/L"
    description: Mapped[str] = mapped_column(Text)
    min_stars_required: Mapped[int] = mapped_column(default=0)
    
    region: Mapped["Region"] = relationship(back_populates="levels")
    challenges: Mapped[List["Challenge"]] = relationship(back_populates="level", cascade="all, delete-orphan")


class Challenge(Base):
    """Specific exercises within a level"""
    __tablename__ = "challenges"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    level_id: Mapped[int] = mapped_column(ForeignKey("levels.id"))
    type: Mapped[ChallengeType] = mapped_column()
    
    content_text: Mapped[str] = mapped_column(Text) # The word/sentence to speak
    phonetic_transcription_ipa: Mapped[str] = mapped_column(String(100))
    reference_audio_url: Mapped[str] = mapped_column(String(255))
    
    # Metadata for AI (e.g., focus phonemes "n", "l")
    # Using JSON type for flexibility
    focus_phonemes: Mapped[dict] = mapped_column(JSON) 
    
    level: Mapped["Level"] = relationship(back_populates="challenges")


# --- Activity & Progress Models ---

class Attempt(Base):
    """A user's attempt at a challenge"""
    __tablename__ = "attempts"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    challenge_id: Mapped[int] = mapped_column(ForeignKey("challenges.id"))
    
    audio_url: Mapped[str] = mapped_column(String(255)) # Uploaded attempt
    score: Mapped[int] = mapped_column() # 0-100 or 1-3 stars
    
    # Detailed AI Analysis Result
    ai_analysis_json: Mapped[dict] = mapped_column(JSON) # { "phoneme_scores": {...}, "pitch_data": ... }
    is_passed: Mapped[bool] = mapped_column(default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    user: Mapped["User"] = relationship(back_populates="attempts")


# --- Education/Classroom Models ---

class Classroom(Base):
    __tablename__ = "classrooms"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    educator_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    name: Mapped[str] = mapped_column(String(100))
    code: Mapped[str] = mapped_column(String(10), unique=True, index=True) # For students to join
    
    members: Mapped[List["ClassMember"]] = relationship(back_populates="classroom", cascade="all, delete-orphan")


class ClassMember(Base):
    __tablename__ = "class_members"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    classroom_id: Mapped[int] = mapped_column(ForeignKey("classrooms.id"))
    student_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    joined_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    
    classroom: Mapped["Classroom"] = relationship(back_populates="members")
    student: Mapped["User"] = relationship(back_populates="class_memberships")


# --- Gamification Extras ---

class Badge(Base):
    __tablename__ = "badges"
    
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String(100))
    icon_url: Mapped[str] = mapped_column(String(255))
    criteria_json: Mapped[dict] = mapped_column(JSON)

class UserBadge(Base):
    __tablename__ = "user_badges"
    
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), primary_key=True)
    badge_id: Mapped[int] = mapped_column(ForeignKey("badges.id"), primary_key=True)
    earned_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
