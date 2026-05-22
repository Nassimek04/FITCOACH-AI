from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from collections import Counter
from app.models.history import WorkoutHistory
from app.schemas.history import HistoryCreate, StatsResponse

def add_history(db: Session, data: HistoryCreate, user_id: int) -> WorkoutHistory:
    entry = WorkoutHistory(**data.model_dump(), user_id=user_id)
    db.add(entry); db.commit(); db.refresh(entry)
    return entry

def get_history(db: Session, user_id: int):
    return db.query(WorkoutHistory).filter(
        WorkoutHistory.user_id == user_id
    ).order_by(WorkoutHistory.completed_at.desc()).all()

def get_stats(db: Session, user_id: int) -> StatsResponse:
    all_entries = db.query(WorkoutHistory).filter(WorkoutHistory.user_id == user_id).all()
    week_ago = datetime.utcnow() - timedelta(days=7)
    week_entries = [e for e in all_entries if e.completed_at >= week_ago]
    total_calories = sum(e.calories_burned or 0 for e in all_entries)
    total_minutes = sum(e.duration_minutes or 0 for e in all_entries)
    avg = total_minutes / len(all_entries) if all_entries else 0
    favorite = Counter([e.workout_name for e in all_entries]).most_common(1)[0][0] if all_entries else None
    return StatsResponse(
        total_workouts=len(all_entries), total_minutes=total_minutes,
        total_calories=total_calories, this_week_workouts=len(week_entries),
        favorite_workout=favorite, average_duration=round(avg, 1)
    )
