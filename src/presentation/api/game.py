from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from src.infrastructure.database.connection import get_db
from src.application.services.game_service import GameService
from src.application.services.auth_service import AuthService
from src.presentation.api.auth import router as auth_router # We might need auth dependency later
# But for now, we'll assuming Getting Map is public or protected?
# Usually Game APIs are protected. Let's start with Public for ease of testing or Protected if we have clean way.
# Let's make it public for now to test easily, or assume client sends token.
# Detailed plan said "Game APIs".

from src.presentation.schemas.game_schema import RegionMapResponse, LevelContentResponse
from src.presentation.schemas.common import BaseResponse

router = APIRouter(prefix="/game", tags=["Game Core"])

@router.get("/map", response_model=BaseResponse[List[RegionMapResponse]])
def get_world_map(db: Session = Depends(get_db)):
    """
    Get the full World Map structure (Regions -> Levels).
    Used by Unity 'Map Scene'.
    """
    service = GameService(db)
    return BaseResponse(
        message="World map fetched successfully",
        data=service.get_world_map()
    )

@router.get("/levels/{level_id}/content", response_model=BaseResponse[LevelContentResponse])
def get_level_content(level_id: int, db: Session = Depends(get_db)):
    """
    Get detailed content (challenges) for a specific level.
    Used when loading a Level in Unity.
    """
    service = GameService(db)
    content = service.get_level_content(level_id)
    if not content:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Level not found"
        )
    return BaseResponse(
        message="Level content fetched successfully",
        data=content
    )
