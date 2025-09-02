import logging
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Dict

from fastapi import FastAPI, Response, HTTPException
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

try:  # optional logging dependency
    from pythonjsonlogger import jsonlogger
except Exception:  # pragma: no cover
    jsonlogger = None

from .evaluator import run_evaluation
from .metrics import ERRORS_TOTAL
from .schemas import EvalStartRequest, EvalStatus


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
    if jsonlogger is not None:
        formatter = jsonlogger.JsonFormatter(
            "%(asctime)s %(levelname)s %(message)s %(component)s %(run_id)s %(eval_id)s",
            rename_fields={"asctime": "ts", "levelname": "level", "message": "msg"},
        )
    else:
        formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
    handler.setFormatter(formatter)
    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.addFilter(ContextFilter(component))
    root.setLevel(logging.INFO)


runs: Dict[str, EvalStatus] = {}
executor = ThreadPoolExecutor(max_workers=1)


def create_app() -> FastAPI:
    setup_logging("eval-offline")
    app = FastAPI()

    @app.get("/healthz")
    def healthz() -> dict[str, str]:
        return {"status": "ok"}

    @app.get("/metrics")
    def metrics() -> Response:  # pragma: no cover - simple
        return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

    @app.post("/runner/eval/start")
    def start(req: EvalStartRequest) -> dict[str, str]:
        eval_id = uuid.uuid4().hex
        runs[eval_id] = EvalStatus(eval_id=eval_id, status="running")

        def _run() -> None:
            try:
                metrics, outdir = run_evaluation(
                    model_id=req.model_id,
                    dataset=req.dataset_id,
                    metrics=req.metrics,
                    batch_size=req.batch_size,
                    limit=req.limit,
                    seed=req.seed,
                    outdir=None,
                    eval_id=eval_id,
                )
                runs[eval_id] = EvalStatus(
                    eval_id=eval_id,
                    status="completed",
                    metrics=metrics,
                    report_uri=str(Path(outdir) / "report.json"),
                )
            except Exception:
                ERRORS_TOTAL.labels(type="other").inc()
                runs[eval_id] = EvalStatus(eval_id=eval_id, status="error")

        executor.submit(_run)
        return {"evalId": eval_id}

    @app.get("/runner/eval/{eval_id}", response_model=EvalStatus)
    def status(eval_id: str) -> EvalStatus:
        state = runs.get(eval_id)
        if state is None:
            raise HTTPException(status_code=404, detail="unknown evalId")
        return state

    return app


app = create_app()
