from typing import List, Optional
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import select
from src.infrastructure.database.models import Region, Level, Challenge

class CurriculumRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_all_regions_with_levels(self) -> List[Region]:
        """
        Fetch all regions, eager loading their levels to avoid N+1 queries.
        Used for the Map Scene.
        """
        # Order by region ID or maybe specific order if added
        stmt = (
            select(Region)
            .options(joinedload(Region.levels))
            .order_by(Region.id)
        )
        return self.db.scalars(stmt).unique().all()

    def get_level_by_id(self, level_id: int) -> Optional[Level]:
        """
        Fetch a single level with all its challenges.
        Used when entering a level.
        """
        stmt = (
            select(Level)
            .options(joinedload(Level.challenges))
            .where(Level.id == level_id)
        )
        return self.db.scalar(stmt)
