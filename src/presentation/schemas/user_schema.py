from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from enum import Enum

class UserRole(str, Enum):
    PLAYER = "player"
    EDUCATOR = "educator"
    ADMIN = "admin"

# --- Shared Properties ---
class UserBase(BaseModel):
    email: EmailStr
    username: str

# --- Properties to receive via API on creation ---
class UserCreate(UserBase):
    password: str = Field(..., min_length=6)
    role: UserRole = UserRole.PLAYER # Default role

# --- Properties to return to client ---
class UserResponse(UserBase):
    id: int
    is_active: bool
    role: UserRole

    class Config:
        from_attributes = True

# --- Token Schemas ---
class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    user_id: Optional[int] = None

# --- Password Reset Schemas ---
class PasswordResetRequest(BaseModel):
    email: EmailStr

class PasswordResetConfirm(BaseModel):
    email: EmailStr
    otp: str = Field(..., min_length=6, max_length=6)
    new_password: str = Field(..., min_length=6)
