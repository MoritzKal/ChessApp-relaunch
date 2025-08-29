import json, logging, sys, time


class JsonFormatter(logging.Formatter):
    def format(self, record):
        base = {
            "ts": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "level": record.levelname.lower(),
            "message": record.getMessage(),
        }
        if hasattr(record, "mdc"):
            base.update(record.mdc)
        return json.dumps(base, ensure_ascii=False)


def setup_json_logging():
    h = logging.StreamHandler(sys.stdout)
    h.setFormatter(JsonFormatter())
    root = logging.getLogger()
    root.handlers = [h]
    root.setLevel(logging.INFO)


def log_event(event: str, **mdc):
    logging.getLogger(__name__).info(event, extra={"mdc": mdc})
