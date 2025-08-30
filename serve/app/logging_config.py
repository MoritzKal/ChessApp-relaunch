import contextvars
import datetime
import json
import logging

log_context: contextvars.ContextVar[dict] = contextvars.ContextVar(
    "log_context", default={}
)


def bind_context(**kwargs) -> None:
    ctx = log_context.get().copy()
    ctx.update({k: v for k, v in kwargs.items() if v is not None})
    log_context.set(ctx)


def reset_context() -> None:
    log_context.set({})


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:  # type: ignore[override]
        data = {
            "ts": datetime.datetime.utcnow().isoformat() + "Z",
            "level": record.levelname.lower(),
            "msg": record.getMessage(),
            "component": "serve",
        }
        ctx = log_context.get()
        for key in [
            "run_id",
            "dataset_id",
            "model_id",
            "model_version",
            "username",
            "path",
            "method",
            "status",
        ]:
            data[key] = ctx.get(key)
        return json.dumps(data)


class ContextFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:  # type: ignore[override]
        ctx = log_context.get()
        record.component = "serve"
        for key in [
            "run_id",
            "dataset_id",
            "model_id",
            "model_version",
            "username",
            "path",
            "method",
            "status",
        ]:
            setattr(record, key, ctx.get(key))
        return True


def setup_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter())
    handler.addFilter(ContextFilter())
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(logging.INFO)
