from typing import List, Optional
from sqlalchemy.orm import Session
from src.infrastructure.database.repositories.curriculum_repository import CurriculumRepository
from src.presentation.schemas.game_schema import RegionMapResponse, LevelContentResponse

class GameService:
    def __init__(self, db: Session):
        self.repository = CurriculumRepository(db)

    def get_world_map(self) -> List[RegionMapResponse]:
        """
        Get the full hierarchy of Regions -> Levels.
        """
        regions = self.repository.get_all_regions_with_levels()
        # SQLAlchemy models map automatically to Pydantic if field names match
        # thanks to configured from_attributes=True
        return [RegionMapResponse.model_validate(r) for r in regions]

    def get_level_content(self, level_id: int) -> Optional[LevelContentResponse]:
        """
        Get a specific Level with all its Challenges.
        """
        level = self.repository.get_level_by_id(level_id)
        if not level:
            return None
        return LevelContentResponse.model_validate(level)
