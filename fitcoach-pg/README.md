# 🏋️ FitCoach AI — Own Backend (FastAPI + PostgreSQL)

## Stack
- **FastAPI** (Python) — Framework REST
- **PostgreSQL** (Docker) — Base de données
- **SQLAlchemy** — ORM
- **JWT** — Authentification
- **Ollama + Gemma 3 12B** — Coach IA

## Démarrage

### 1. Lance PostgreSQL (Docker)
```bash
docker run --name fitcoach-db \
  -e POSTGRES_USER=fitcoach \
  -e POSTGRES_PASSWORD=fitcoach123 \
  -e POSTGRES_DB=fitcoach_db \
  -p 5432:5432 -d postgres:15
```

### 2. Installe et lance FastAPI
```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 3. Teste
Ouvre : **http://localhost:8000/docs**

## Endpoints
| Route | Méthode | Description |
|---|---|---|
| `/auth/register` | POST | Créer un compte |
| `/auth/login` | POST | Login → JWT |
| `/auth/me` | GET/PUT | Profil |
| `/workouts/` | GET/POST | Workouts |
| `/workouts/{id}` | PUT/DELETE | Modifier/Supprimer |
| `/runs/` | GET/POST | Courses GPS |
| `/runs/stats` | GET | Stats courses |
| `/history/` | GET/POST | Historique |
| `/history/stats` | GET | Stats globales |
| `/chat/` | POST | Chat Ollama |
| `/health` | GET | Health check |
