import logging
from fastapi import FastAPI, Response
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
from pythonjsonlogger import jsonlogger


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
    setup_logging("eval-offline")
    app = FastAPI()

    @app.get("/healthz")
    def healthz() -> dict[str, str]:
        return {"status": "ok"}

    @app.get("/metrics")
    def metrics() -> Response:  # pragma: no cover - simple
        return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

    return app


app = create_app()
