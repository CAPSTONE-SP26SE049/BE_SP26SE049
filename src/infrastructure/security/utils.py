from datetime import datetime, timedelta
from typing import Optional, Union, Any
from jose import jwt
import bcrypt  # Direct bcrypt usage
from src.infrastructure.config.settings import settings

# 1. Hashing Functions (Replaced passlib with bcrypt direct)
def verify_password(plain_password: str, hashed_password: str) -> bool:
    # bcrypt requires bytes
    try:
        if isinstance(plain_password, str):
            plain_password = plain_password.encode("utf-8")
        if isinstance(hashed_password, str):
            hashed_password = hashed_password.encode("utf-8")
            
        return bcrypt.checkpw(plain_password, hashed_password)
    except Exception:
        # If hash is invalid format
        return False

def get_password_hash(password: str) -> str:
    if isinstance(password, str):
        password = password.encode("utf-8")
    
    # Generate salt and hash
    hashed = bcrypt.hashpw(password, bcrypt.gensalt())
    return hashed.decode("utf-8") # Return as string for DB storage

# 2. JWT Functions
def create_access_token(subject: Union[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode = {"exp": expire, "sub": str(subject)}
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt

def create_reset_token(email: str, expires_delta: Optional[timedelta] = None) -> str:
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15) # Short expiry for reset
    
    # Store email in subject with prefix to distinguish from login token
    to_encode = {"exp": expire, "sub": f"reset:{email}"}
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt

def decode_token(token: str) -> Optional[str]:
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        return payload.get("sub")
    except jwt.JWTError:
        return None

def generate_otp() -> str:
    import random
    import string
    # Generate 6-digit numeric OTP
    return "".join(random.choices(string.digits, k=6))
