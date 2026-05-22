from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from typing import List
from app.schemas.workout import WorkoutCreate, WorkoutResponse, WorkoutUpdate
from app.services.workout_service import create_workout, get_workouts, get_workout, update_workout, delete_workout
from app.services.auth_service import get_current_user
from app.database import get_db

router = APIRouter(prefix="/workouts", tags=["💪 Workouts"])

@router.get("/", response_model=List[WorkoutResponse])
def list_workouts(db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_workouts(db, user.id)

@router.post("/", response_model=WorkoutResponse)
def create(data: WorkoutCreate, db: Session = Depends(get_db), user=Depends(get_current_user)):
    return create_workout(db, data, user.id)

@router.get("/{workout_id}", response_model=WorkoutResponse)
def detail(workout_id: int, db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_workout(db, workout_id, user.id)

@router.put("/{workout_id}", response_model=WorkoutResponse)
def update(workout_id: int, data: WorkoutUpdate, db: Session = Depends(get_db), user=Depends(get_current_user)):
    return update_workout(db, workout_id, user.id, data)

@router.delete("/{workout_id}")
def delete(workout_id: int, db: Session = Depends(get_db), user=Depends(get_current_user)):
    delete_workout(db, workout_id, user.id)
    return {"message": "Workout supprimé"}
