import argparse
import logging
import uuid

try:
    from pythonjsonlogger import jsonlogger
except Exception:  # pragma: no cover - optional dependency
    jsonlogger = None

from app.evaluator import run_evaluation


def setup_logging() -> None:
    handler = logging.StreamHandler()
    if jsonlogger is not None:
        formatter = jsonlogger.JsonFormatter(
            "%(asctime)s %(levelname)s %(message)s %(component)s %(eval_id)s",
            rename_fields={"asctime": "ts", "levelname": "level", "message": "msg"},
        )
    else:
        formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
    handler.setFormatter(formatter)
    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(logging.INFO)


def main() -> int:
    parser = argparse.ArgumentParser(description="Offline evaluation CLI")
    parser.add_argument("--model-id", required=True)
    parser.add_argument("--dataset", required=False)
    parser.add_argument("--metrics", nargs="*", default=None)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--limit", type=int, default=200)
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--outdir", default=None)
    args = parser.parse_args()

    setup_logging()
    eval_id = uuid.uuid4().hex
    outdir = args.outdir or f"artifacts/eval/{eval_id}"
    logging.info("start", extra={"component": "eval-offline", "eval_id": eval_id})
    try:
        run_evaluation(
            model_id=args.model_id,
            dataset=args.dataset,
            metrics=args.metrics,
            batch_size=args.batch_size,
            limit=args.limit,
            seed=args.seed,
            outdir=outdir,
            eval_id=eval_id,
        )
        logging.info("completed", extra={"component": "eval-offline", "eval_id": eval_id})
        return 0
    except Exception:  # pragma: no cover - defensive
        logging.exception("evaluation failed", extra={"component": "eval-offline", "eval_id": eval_id})
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
