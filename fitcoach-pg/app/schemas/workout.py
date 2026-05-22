from pydantic import BaseModel
from typing import Optional, List, Any
from datetime import datetime

class WorkoutCreate(BaseModel):
    name: str
    description: Optional[str] = None
    difficulty: str = "beginner"
    duration_minutes: int
    category: str
    exercises: Optional[List[Any]] = []

class WorkoutUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    difficulty: Optional[str] = None
    duration_minutes: Optional[int] = None
    category: Optional[str] = None

class WorkoutResponse(BaseModel):
    id: int
    name: str
    description: Optional[str]
    difficulty: str
    duration_minutes: int
    category: str
    exercises: Optional[List[Any]]
    created_at: datetime
    owner_id: int
    class Config:
        from_attributes = True
