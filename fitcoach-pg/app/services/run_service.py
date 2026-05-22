from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from app.models.run import RunSession
from app.schemas.run import RunCreate, RunStatsResponse
from fastapi import HTTPException

def save_run(db: Session, data: RunCreate, user_id: int) -> RunSession:
    run = RunSession(**data.model_dump(), user_id=user_id)
    db.add(run); db.commit(); db.refresh(run)
    return run

def get_runs(db: Session, user_id: int):
    return db.query(RunSession).filter(RunSession.user_id == user_id).order_by(RunSession.completed_at.desc()).all()

def get_run(db: Session, run_id: int, user_id: int) -> RunSession:
    run = db.query(RunSession).filter(RunSession.id == run_id, RunSession.user_id == user_id).first()
    if not run: raise HTTPException(status_code=404, detail="Course introuvable")
    return run

def delete_run(db: Session, run_id: int, user_id: int):
    run = get_run(db, run_id, user_id)
    db.delete(run); db.commit()

def get_run_stats(db: Session, user_id: int) -> RunStatsResponse:
    all_runs = db.query(RunSession).filter(RunSession.user_id == user_id).all()
    week_ago = datetime.utcnow() - timedelta(days=7)
    week_runs = [r for r in all_runs if r.completed_at >= week_ago]
    total_km = sum(r.distance_km or 0 for r in all_runs)
    total_cal = sum(r.calories_burned or 0 for r in all_runs)
    total_time = sum(r.duration_seconds or 0 for r in all_runs)
    best = max((r.distance_km or 0 for r in all_runs), default=0)
    speeds = [r.avg_speed_kmh for r in all_runs if r.avg_speed_kmh]
    avg_speed = sum(speeds) / len(speeds) if speeds else 0
    return RunStatsResponse(
        total_runs=len(all_runs), total_km=round(total_km, 2),
        total_calories=round(total_cal, 1), total_time_seconds=total_time,
        avg_speed_kmh=round(avg_speed, 1), best_distance_km=round(best, 2),
        this_week_runs=len(week_runs),
        this_week_km=round(sum(r.distance_km or 0 for r in week_runs), 2)
    )
