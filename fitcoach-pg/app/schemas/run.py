from pydantic import BaseModel
from typing import Optional, List, Any
from datetime import datetime

class RunCreate(BaseModel):
    distance_km: float = 0.0
    duration_seconds: int = 0
    avg_speed_kmh: float = 0.0
    max_speed_kmh: float = 0.0
    calories_burned: float = 0.0
    route_points: Optional[List[Any]] = []
    start_lat: Optional[float] = None
    start_lng: Optional[float] = None
    weather: Optional[str] = None
    temperature: Optional[float] = None
    notes: Optional[str] = None

class RunResponse(BaseModel):
    id: int
    distance_km: float
    duration_seconds: int
    avg_speed_kmh: float
    max_speed_kmh: float
    calories_burned: float
    route_points: Optional[List[Any]]
    weather: Optional[str]
    temperature: Optional[float]
    completed_at: datetime
    class Config:
        from_attributes = True

class RunStatsResponse(BaseModel):
    total_runs: int
    total_km: float
    total_calories: float
    total_time_seconds: int
    avg_speed_kmh: float
    best_distance_km: float
    this_week_runs: int
    this_week_km: float
