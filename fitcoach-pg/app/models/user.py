from sqlalchemy import Column, Integer, String, Float, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    username = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    full_name = Column(String)
    age = Column(Integer)
    weight = Column(Float)
    height = Column(Float)
    fitness_goal = Column(String)
    created_at = Column(DateTime, default=datetime.utcnow)

    workouts = relationship("Workout", back_populates="owner")
    history = relationship("WorkoutHistory", back_populates="user")
    runs = relationship("RunSession", back_populates="user")
