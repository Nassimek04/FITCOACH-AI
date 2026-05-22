from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from typing import List
from app.schemas.history import HistoryCreate, HistoryResponse, StatsResponse
from app.services.history_service import add_history, get_history, get_stats
from app.services.auth_service import get_current_user
from app.database import get_db

router = APIRouter(prefix="/history", tags=["📊 Historique"])

@router.post("/", response_model=HistoryResponse)
def log(data: HistoryCreate, db: Session = Depends(get_db), user=Depends(get_current_user)):
    return add_history(db, data, user.id)

@router.get("/", response_model=List[HistoryResponse])
def list_history(db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_history(db, user.id)

@router.get("/stats", response_model=StatsResponse)
def stats(db: Session = Depends(get_db), user=Depends(get_current_user)):
    return get_stats(db, user.id)
