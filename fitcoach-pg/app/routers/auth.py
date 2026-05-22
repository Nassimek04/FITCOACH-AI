from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.schemas.user import UserCreate, UserLogin, UserResponse, UserUpdate, Token
from app.services.auth_service import register_user, login_user, get_current_user, update_user
from app.database import get_db

router = APIRouter(prefix="/auth", tags=["🔐 Auth"])

@router.post("/register", response_model=Token)
def register(data: UserCreate, db: Session = Depends(get_db)):
    user = register_user(db, data)
    from app.core.security import create_access_token
    token = create_access_token({"sub": str(user.id)})
    return {"access_token": token, "token_type": "bearer", "user": user}

@router.post("/login", response_model=Token)
def login(data: UserLogin, db: Session = Depends(get_db)):
    token, user = login_user(db, data.email, data.password)
    return {"access_token": token, "token_type": "bearer", "user": user}

@router.get("/me", response_model=UserResponse)
def me(user=Depends(get_current_user)):
    return user

@router.put("/me", response_model=UserResponse)
def update_profile(data: UserUpdate, user=Depends(get_current_user), db: Session = Depends(get_db)):
    return update_user(db, user, data)
