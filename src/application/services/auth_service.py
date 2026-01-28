from datetime import datetime, timedelta
from typing import Optional
from sqlalchemy.orm import Session
from src.infrastructure.database.repositories.user_repository import UserRepository
from src.infrastructure.security.utils import (
    verify_password, get_password_hash, create_access_token, 
    verify_password, get_password_hash, create_access_token, 
    generate_otp
)
from src.domain.entities.user import User as UserEntity
from src.presentation.schemas.user_schema import UserCreate, Token
from src.infrastructure.email.email_service import EmailService
import logging

class AuthService:
    def __init__(self, db: Session):
        self.repository = UserRepository(db)

    def register_user(self, user_in: UserCreate) -> Optional[UserEntity]:
        # 1. Check if user exists
        if self.repository.get_by_email(user_in.email):
            raise ValueError("Email already registered")
        if self.repository.get_by_username(user_in.username):
            raise ValueError("Username already taken")
            
        # 2. Hash Password
        hashed_pw = get_password_hash(user_in.password)
        
        # 3. Create Entity object (temporary for transport)
        user_ent = UserEntity(
            id=None,
            username=user_in.username,
            email=user_in.email,
            role=user_in.role, # Passed from schema
            password_hash="" # Not needed here
        )
        
        # 4. Save to DB
        return self.repository.create(user_ent, hashed_pw)

    def authenticate_user(self, email: str, password: str) -> Optional[Token]:
        # 1. Get User
        user = self.repository.get_by_email(email)
        if not user:
            return None
            
        # 2. Verify Password
        if not verify_password(password, user.password_hash):
            return None
            
        # 3. Create Token
        access_token = create_access_token(subject=user.id)
        return Token(access_token=access_token, token_type="bearer")

    async def forgot_password(self, email: str) -> None:
        """
        Check email and send reset token via Email Service.
        """
        user = self.repository.get_by_email(email)
        if not user:
            # Security: Don't reveal if user exists. 
            # But for this implementation we return dummy or raise vague error. 
            # Or we can just raise "Email not found" for clarity in dev.
            raise ValueError("Email not found")
        
        # Generate OTP
        otp = generate_otp()
        expires_at = datetime.utcnow() + timedelta(minutes=15)
        
        # Save OTP to DB
        self.repository.save_otp(user.id, otp, expires_at)
        
        # Send Email
        try:
            email_service = EmailService()
            await email_service.send_reset_password_email(user.email, otp)
        except Exception as e:
            # Report Recommendation: Log error but don't crash, prevent 500 error
            logging.error(f"Failed to send reset email to {email}: {e}")
            # We still return success to the user to prevent enumeration (and since logic was valid)
            pass

    def reset_password(self, email: str, otp: str, new_password: str) -> bool:
        """
        Verify OTP and reset password.
        """
        user = self.repository.get_by_email_and_otp(email, otp)
        
        if not user:
            # Either email invalid OR code invalid/mismatched
            raise ValueError("Invalid email or code")

        # Check Expiry
        if not user.reset_expires_at or user.reset_expires_at < datetime.utcnow():
             raise ValueError("Code expired")
            
        hashed_pw = get_password_hash(new_password)
        self.repository.update_password(user.id, hashed_pw)
        return True
