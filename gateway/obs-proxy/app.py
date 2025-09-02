import logging
import time
from typing import Optional

from fastapi import Depends, FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pythonjsonlogger import jsonlogger
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from config import get_settings
from security import client_ip, require_api_key
from proxy_clients import forward_get, loki_client, prom_client

settings = get_settings()

logger = logging.getLogger("obs-proxy")
handler = logging.StreamHandler()
handler.setFormatter(jsonlogger.JsonFormatter())
logger.addHandler(handler)
logger.setLevel(settings.LOG_LEVEL)

limiter = Limiter(key_func=client_ip, default_limits=[settings.RATE_LIMIT])

app = FastAPI(title="ChessApp Obs Proxy", version="v1", root_path="")
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

origins = [o.strip() for o in settings.CORS_ALLOW_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_methods=["GET"],
    allow_headers=["*"],
)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    duration_ms = (time.time() - start) * 1000
    logger.info(
        "request",
        extra={
            "method": request.method,
            "path": request.url.path,
            "status": response.status_code,
            "duration_ms": round(duration_ms, 2),
        },
    )
    return response


enforce_key = require_api_key


@app.get("/healthz")
async def healthz():
    return {"status": "ok"}


# Prometheus
@app.get("/obs/prom/query", dependencies=[Depends(enforce_key)])
@limiter.limit(settings.RATE_LIMIT)
async def prom_query(query: str):
    settings = get_settings()
    async with prom_client(settings) as client:
        status, data, headers = await forward_get(client, "/api/v1/query", {"query": query})
    return JSONResponse(content=data, status_code=status, headers=headers)


@app.get("/obs/prom/range", dependencies=[Depends(enforce_key)])
@limiter.limit(settings.RATE_LIMIT)
async def prom_range(query: str, start: str, end: str, step: str):
    settings = get_settings()
    params = {"query": query, "start": start, "end": end, "step": step}
    async with prom_client(settings) as client:
        status, data, headers = await forward_get(client, "/api/v1/query_range", params)
    return JSONResponse(content=data, status_code=status, headers=headers)


# Loki
@app.get("/obs/loki/query", dependencies=[Depends(enforce_key)])
@limiter.limit(settings.RATE_LIMIT)
async def loki_query(
    query: str,
    limit: Optional[int] = None,
    direction: Optional[str] = None,
    start: Optional[str] = None,
    end: Optional[str] = None,
):
    settings = get_settings()
    params = {"query": query}
    if limit is not None:
        params["limit"] = limit
    if direction is not None:
        params["direction"] = direction
    if start is not None:
        params["start"] = start
    if end is not None:
        params["end"] = end
    async with loki_client(settings) as client:
        status, data, headers = await forward_get(client, "/loki/api/v1/query", params)
    return JSONResponse(content=data, status_code=status, headers=headers)


@app.get("/obs/loki/range", dependencies=[Depends(enforce_key)])
@limiter.limit(settings.RATE_LIMIT)
async def loki_range(
    query: str,
    limit: Optional[int] = None,
    direction: Optional[str] = None,
    start: Optional[str] = None,
    end: Optional[str] = None,
):
    settings = get_settings()
    params = {"query": query}
    if limit is not None:
        params["limit"] = limit
    if direction is not None:
        params["direction"] = direction
    if start is not None:
        params["start"] = start
    if end is not None:
        params["end"] = end
    async with loki_client(settings) as client:
        status, data, headers = await forward_get(
            client, "/loki/api/v1/query_range", params
        )
    return JSONResponse(content=data, status_code=status, headers=headers)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app:app", host="0.0.0.0", port=8088)
