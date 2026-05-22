from sqlalchemy.orm import Session
from fastapi import HTTPException
from app.models.workout import Workout
from app.schemas.workout import WorkoutCreate, WorkoutUpdate

def create_workout(db: Session, data: WorkoutCreate, user_id: int) -> Workout:
    workout = Workout(**data.model_dump(), owner_id=user_id)
    db.add(workout); db.commit(); db.refresh(workout)
    return workout

def get_workouts(db: Session, user_id: int):
    return db.query(Workout).filter(Workout.owner_id == user_id).all()

def get_workout(db: Session, workout_id: int, user_id: int) -> Workout:
    w = db.query(Workout).filter(Workout.id == workout_id, Workout.owner_id == user_id).first()
    if not w: raise HTTPException(status_code=404, detail="Workout introuvable")
    return w

def update_workout(db: Session, workout_id: int, user_id: int, data: WorkoutUpdate) -> Workout:
    w = get_workout(db, workout_id, user_id)
    for field, value in data.model_dump(exclude_none=True).items():
        setattr(w, field, value)
    db.commit(); db.refresh(w)
    return w

def delete_workout(db: Session, workout_id: int, user_id: int):
    w = get_workout(db, workout_id, user_id)
    db.delete(w); db.commit()
