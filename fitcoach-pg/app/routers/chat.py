from fastapi import APIRouter, Depends, HTTPException
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.ollama_service import chat_with_ollama, check_ollama_status
from app.services.auth_service import get_current_user
from app.core.config import settings

router = APIRouter(prefix="/chat", tags=["🤖 Chat IA"])

@router.post("/", response_model=ChatResponse)
async def chat(data: ChatRequest, user=Depends(get_current_user)):
    try:
        response = await chat_with_ollama(data.message, data.history or [])
        return ChatResponse(response=response, model=settings.OLLAMA_MODEL)
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Serveur IA indisponible: {str(e)}")

@router.get("/status")
async def status():
    alive = await check_ollama_status()
    return {"ollama_online": alive, "model": settings.OLLAMA_MODEL}
