from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from typing import List
from app.schemas.run import RunCreate, RunResponse, RunStatsResponse
from app.services.run_service import save_run, get_runs, get_run, delete_run, get_run_stats
from app.services.auth_service import get_current_user
from app.database import get_db

router = APIRouter(prefix="/runs", tags=["🏃 Course GPS"])

@router.post("/", response_model=RunResponse)
def save(data: RunCreate, db: Session = Depends(get_db), user=Depends(get_current_user)):
    return save_run(db, data, user.id)

@router.get("/", response_model=List[RunResponse])
def list_runs(db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_runs(db, user.id)

@router.get("/stats", response_model=RunStatsResponse)
def stats(db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_run_stats(db, user.id)

@router.get("/{run_id}", response_model=RunResponse)
def detail(run_id: int, db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_run(db, run_id, user.id)

@router.delete("/{run_id}")
def delete(run_id: int, db: Session = Depends(get_db), user=Depends(get_current_user)):
    delete_run(db, run_id, user.id)
    return {"message": "Course supprimée"}
