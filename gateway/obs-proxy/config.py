from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROM_BASE_URL: str
    LOKI_BASE_URL: str
    OBS_API_KEY: str | None = None
    CORS_ALLOW_ORIGINS: str = "http://localhost:5173"
    RATE_LIMIT: str = "60/minute"
    TIMEOUT_SECONDS: float = 3.0
    RETRIES: int = 2
    LOG_LEVEL: str = "INFO"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


@lru_cache
def get_settings() -> Settings:
    return Settings()
