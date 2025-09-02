from fastapi import HTTPException, Request, status

from .config import get_settings


async def require_api_key(request: Request) -> None:
    settings = get_settings()
    if settings.OBS_API_KEY:
        api_key = request.headers.get("X-Obs-Api-Key")
        if api_key != settings.OBS_API_KEY:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid API Key"
            )


def client_ip(request: Request) -> str:
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",")[0].strip()
    client = request.client
    return client.host if client else "unknown"
