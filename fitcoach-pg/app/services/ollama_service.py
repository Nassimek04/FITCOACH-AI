import httpx, re
from app.core.config import settings
from app.schemas.chat import ChatMessage

SYSTEM_PROMPT = """Tu es FitCoach AI, un coach sportif expert, motivant et bienveillant.
Tu donnes des conseils pratiques sur les entraînements, la nutrition et le mode de vie sain.
Tu t'adaptes à la langue de l'utilisateur (français, anglais ou arabe).
Filtre tout tag <think> de tes réponses."""

async def chat_with_ollama(message: str, history: list[ChatMessage]) -> str:
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    for msg in history[-10:]:
        messages.append({"role": msg.role, "content": msg.content})
    messages.append({"role": "user", "content": message})

    async with httpx.AsyncClient(timeout=120.0) as client:
        response = await client.post(
            f"{settings.OLLAMA_BASE_URL}/api/chat",
            json={"model": settings.OLLAMA_MODEL, "messages": messages, "stream": False},
        )
        response.raise_for_status()
        content = response.json()["message"]["content"]
        return re.sub(r'<think>.*?</think>', '', content, flags=re.DOTALL).strip()

async def check_ollama_status() -> bool:
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            return (await client.get(f"{settings.OLLAMA_BASE_URL}/api/tags")).status_code == 200
    except:
        return False
