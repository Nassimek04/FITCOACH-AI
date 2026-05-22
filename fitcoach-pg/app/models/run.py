from sqlalchemy import Column, Integer, ForeignKey, DateTime, Float, String, JSON
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class RunSession(Base):
    __tablename__ = "run_sessions"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    distance_km = Column(Float, default=0.0)
    duration_seconds = Column(Integer, default=0)
    avg_speed_kmh = Column(Float, default=0.0)
    max_speed_kmh = Column(Float, default=0.0)
    calories_burned = Column(Float, default=0.0)
    route_points = Column(JSON, default=list)
    start_lat = Column(Float)
    start_lng = Column(Float)
    weather = Column(String)
    temperature = Column(Float)
    notes = Column(String)
    completed_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="runs")
