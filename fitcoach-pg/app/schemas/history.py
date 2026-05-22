from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class HistoryCreate(BaseModel):
    workout_id: Optional[int] = None
    workout_name: str
    duration_minutes: int
    calories_burned: Optional[float] = None
    notes: Optional[str] = None

class HistoryResponse(BaseModel):
    id: int
    workout_name: str
    duration_minutes: int
    calories_burned: Optional[float]
    notes: Optional[str]
    completed_at: datetime
    class Config:
        from_attributes = True

class StatsResponse(BaseModel):
    total_workouts: int
    total_minutes: int
    total_calories: float
    this_week_workouts: int
    favorite_workout: Optional[str]
    average_duration: float
