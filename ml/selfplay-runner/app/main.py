import logging
import asyncio
from fastapi import FastAPI, Response, HTTPException
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
from pythonjsonlogger import jsonlogger

from .runner import SelfPlayRunner
from .schemas import RunStatus, StartRequest
from .serve_client import ServeClient


class ContextFilter(logging.Filter):
    """Adds default logging fields."""

    def __init__(self, component: str):
        super().__init__()
        self.component = component

    def filter(self, record: logging.LogRecord) -> bool:  # pragma: no cover - simple
        if not hasattr(record, "component"):
            record.component = self.component
        if not hasattr(record, "run_id"):
            record.run_id = None
        if not hasattr(record, "eval_id"):
            record.eval_id = None
        return True


def setup_logging(component: str) -> None:
    handler = logging.StreamHandler()
    formatter = jsonlogger.JsonFormatter(
        "%(asctime)s %(levelname)s %(message)s %(component)s %(run_id)s %(eval_id)s",
        rename_fields={"asctime": "ts", "levelname": "level", "message": "msg"},
    )
    handler.setFormatter(formatter)
    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.addFilter(ContextFilter(component))
    root.setLevel(logging.INFO)


def create_app() -> FastAPI:
    setup_logging("selfplay-runner")
    app = FastAPI()

    client = ServeClient()
    runner = SelfPlayRunner(client)

    @app.get("/healthz")
    def healthz() -> dict[str, str]:
        return {"status": "ok"}

    @app.get("/metrics")
    def metrics() -> Response:  # pragma: no cover - simple
        return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

    @app.post("/runner/selfplay/start")
    def start(req: StartRequest) -> dict[str, str]:
        run_id = runner.start(req)
        return {"runId": run_id}

    @app.get("/runner/selfplay/runs/{run_id}", response_model=RunStatus)
    def status(run_id: str) -> RunStatus:
        st = runner.get_status(run_id)
        if not st:
            raise HTTPException(status_code=404, detail="run not found")
        return st

    @app.get("/runner/selfplay/debug")
    async def debug(games: int = 4) -> RunStatus:
        run_status = await asyncio.to_thread(runner.run_debug, games)
        return run_status

    return app


app = create_app()
