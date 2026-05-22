@echo off
echo 🏋️ Lancement FitCoach AI Backend (FastAPI + PostgreSQL)...
echo.
echo 1. Verifie que Docker est lance et que fitcoach-db tourne
echo    docker ps
echo.
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
