from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from src.infrastructure.database.connection import get_db
from src.application.services.auth_service import AuthService
from src.presentation.schemas.user_schema import (
    UserCreate, UserResponse, Token, 
    PasswordResetRequest, PasswordResetConfirm
)
from src.presentation.schemas.common import BaseResponse

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/register", response_model=BaseResponse[UserResponse])
def register(user_in: UserCreate, db: Session = Depends(get_db)):
    """Register a new user"""
    service = AuthService(db)
    try:
        new_user = service.register_user(user_in)
        return BaseResponse(
            message="User registered successfully",
            data=new_user
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )

@router.post("/login", response_model=BaseResponse[Token])
def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    """
    Login for Access Token.
    Note: username field in OAuth2PasswordRequestForm can assume email if frontend sends it there.
    We will try to treat 'form_data.username' as email.
    """
    service = AuthService(db)
    
    # We assume the user sends email in the 'username' field of x-www-form-urlencoded
    token = service.authenticate_user(email=form_data.username, password=form_data.password)
    
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return BaseResponse(
        message="Login successful",
        data=token
    )

@router.post("/forgot-password", response_model=BaseResponse[dict])
async def forgot_password(request: PasswordResetRequest, db: Session = Depends(get_db)):
    """
    Request Password Reset Token.
    Sends an email with the reset link.
    """
    service = AuthService(db)
    try:
        await service.forgot_password(request.email)
        return BaseResponse(
            message="If the email exists, a reset link has been sent.",
            data=None
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )

@router.post("/reset-password", response_model=BaseResponse[dict])
def reset_password(request: PasswordResetConfirm, db: Session = Depends(get_db)):
    """
    Reset password using the Email + OTP.
    """
    service = AuthService(db)
    try:
        service.reset_password(request.email, request.otp, request.new_password)
        return BaseResponse(
            message="Password updated successfully",
            data=None
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
