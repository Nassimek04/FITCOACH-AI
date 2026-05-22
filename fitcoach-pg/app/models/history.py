from sqlalchemy import Column, Integer, ForeignKey, DateTime, Float, String, JSON
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class WorkoutHistory(Base):
    __tablename__ = "workout_history"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    workout_id = Column(Integer, ForeignKey("workouts.id"), nullable=True)
    workout_name = Column(String, nullable=False)
    duration_minutes = Column(Integer)
    calories_burned = Column(Float)
    notes = Column(String)
    completed_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="history")
    workout = relationship("Workout", back_populates="history")
