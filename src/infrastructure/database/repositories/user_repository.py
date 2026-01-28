from typing import Optional
from sqlalchemy.orm import Session
from sqlalchemy import select
from datetime import datetime
from src.infrastructure.database.models import User, UserProfile
from src.domain.entities.user import User as UserEntity, UserRole

class UserRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_by_email(self, email: str) -> Optional[User]:
        stmt = select(User).where(User.email == email)
        return self.db.scalar(stmt)

    def get_by_username(self, username: str) -> Optional[User]:
        stmt = select(User).where(User.username == username)
        return self.db.scalar(stmt)

    def create(self, user_data: UserEntity, password_hash: str) -> User:
        # Create Core User
        db_user = User(
            username=user_data.username,
            email=user_data.email,
            password_hash=password_hash,
            role=UserRole(user_data.role),
            is_active=True
        )
        self.db.add(db_user)
        self.db.flush() # Flush to get the ID
        
        # Create Request Profile (if player)
        # For simplicity, we create empty profile for every user or check role
        profile = UserProfile(user_id=db_user.id)
        self.db.add(profile)
        
        self.db.commit()
        self.db.refresh(db_user)
        return db_user

    def update_password(self, user_id: int, new_password_hash: str) -> None:
        user = self.db.get(User, user_id)
        if user:
            user.password_hash = new_password_hash
            # Clear OTP after successful reset
            user.reset_code = None
            user.reset_expires_at = None
            self.db.commit()

    def save_otp(self, user_id: int, otp: str, expires_at: datetime) -> None:
        user = self.db.get(User, user_id)
        if user:
            user.reset_code = otp
            user.reset_expires_at = expires_at
            self.db.commit()

    def get_by_email_and_otp(self, email: str, otp: str) -> Optional[User]:
        # Basic check, expiry logic should be in Service
        stmt = select(User).where(User.email == email, User.reset_code == otp)
        return self.db.scalar(stmt)
