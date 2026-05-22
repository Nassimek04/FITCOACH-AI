from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql://fitcoach:fitcoach123@localhost:5432/fitcoach_db"
    SECRET_KEY: str = "fitcoach-super-secret-key-2024"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    OLLAMA_BASE_URL: str = "http://10.148.190.57:11434"
    OLLAMA_MODEL: str = "gemma3:12b"

    class Config:
        env_file = ".env"

settings = Settings()
