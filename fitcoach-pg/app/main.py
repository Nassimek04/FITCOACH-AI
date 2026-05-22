from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.database import Base, engine
from app.routers import auth, workouts, chat, history, runs
from app.models import user, workout, history as history_model, run

# Crée toutes les tables PostgreSQL au démarrage
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="FitCoach AI — Own Backend",
    description="""
## 🏋️ FitCoach AI Backend (FastAPI + PostgreSQL)

Backend personnel développé avec **FastAPI (Python) + PostgreSQL (Docker)**.
Fonctionne en parallèle avec Firebase.

### Fonctionnalités
- 🔐 **Auth** : Register / Login / JWT
- 👤 **Profil** : nom, âge, poids, objectif fitness
- 💪 **Workouts** : CRUD complet
- 🏃 **Courses GPS** : historique + stats
- 📊 **Historique séances** : stats globales
- 🤖 **Chat IA** : proxy Ollama Gemma 3 12B
    """,
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(workouts.router)
app.include_router(runs.router)
app.include_router(history.router)
app.include_router(chat.router)

@app.get("/", tags=["Root"])
def root():
    return {
        "app": "FitCoach AI — Own Backend",
        "status": "running 💪",
        "database": "PostgreSQL (Docker)",
        "docs": "/docs",
        "version": "1.0.0",
        "routes": ["/auth", "/workouts", "/runs", "/history", "/chat"]
    }

@app.get("/health", tags=["Root"])
def health():
    return {"status": "ok", "database": "PostgreSQL", "framework": "FastAPI"}
