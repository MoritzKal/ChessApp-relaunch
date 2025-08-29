#!/usr/bin/env python3
import argparse, json, os, glob, time, uuid, sys
import pandas as pd
import requests


def load_input(path: str) -> pd.DataFrame:
    files = sorted(
        glob.glob(os.path.join(path, "*.jsonl"))
        + glob.glob(os.path.join(path, "*.ndjson"))
    )
    if not files:
        raise FileNotFoundError(f"No *.jsonl found under {path}")
    dfs = [pd.read_json(f, lines=True) for f in files]
    return pd.concat(dfs, ignore_index=True)


def bucket_elo(v: float) -> str:
    if pd.isna(v):
        return "unknown"
    v = float(v)
    if v < 800:
        return "<800"
    if v < 1200:
        return "800-1199"
    if v < 1600:
        return "1200-1599"
    if v < 2000:
        return "1600-1999"
    return "2000+"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", required=True)
    ap.add_argument("--output", required=True)
    ap.add_argument("--dataset-id", required=True)
    ap.add_argument("--run-id", default=None)
    ap.add_argument("--manifest", default="manifest.json")
    ap.add_argument(
        "--push-metrics",
        default=None,
        help="http(s)://host:8000/internal/dataset/metrics",
    )
    args = ap.parse_args()

    run_id = args.run_id or f"local-{uuid.uuid4().hex[:8]}"
    mdc = {
        "dataset_id": args.dataset_id,
        "run_id": run_id,
        "component": "ml.tools.dataset_export",
        "username": os.environ.get("USER", "local"),
    }
    print(json.dumps({"event": "dataset.export_started", **mdc}))

    try:
        t0 = time.time()
        df = load_input(args.input)
        rows = len(df)

        result_counts = (
            df.get("result", pd.Series(dtype="object"))
            .value_counts(dropna=False)
            .to_dict()
        )
        time_cat_counts = (
            df.get("time_category", pd.Series(dtype="object"))
            .value_counts(dropna=False)
            .to_dict()
        )
        if "white_rating" in df.columns or "black_rating" in df.columns:
            avg_elo = df[["white_rating", "black_rating"]].mean(axis=1, skipna=True)
            buckets = avg_elo.apply(bucket_elo).value_counts(dropna=False).to_dict()
        else:
            buckets = {}

        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        df.to_parquet(args.output, index=False)
        elapsed_ms = int((time.time() - t0) * 1000)

        manifest = {
            "dataset_id": args.dataset_id,
            "version": "v0",
            "run_id": run_id,
            "created_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "rows": rows,
            "location": args.output,
            "stats": {
                "result": {str(k): int(v) for k, v in result_counts.items()},
                "time_category": {str(k): int(v) for k, v in time_cat_counts.items()},
                "elo_buckets": {str(k): int(v) for k, v in buckets.items()},
            },
        }
        with open(args.manifest, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2)

        if args.push_metrics:
            try:
                payload = {
                    "dataset_id": args.dataset_id,
                    "run_id": run_id,
                    "rows": rows,
                    "invalid": {"reasons": {}},
                    "export_duration_ms": elapsed_ms,
                }
                resp = requests.post(args.push_metrics, json=payload, timeout=5)
                resp.raise_for_status()
            except Exception as e:
                print(
                    json.dumps(
                        {
                            "event": "dataset.metrics_push_failed",
                            **mdc,
                            "error": str(e),
                        }
                    )
                )

        log = {
            "event": "dataset.export_completed",
            **mdc,
            "rows_total": rows,
            "elapsed_ms": elapsed_ms,
        }
        print(json.dumps(log))
        sys.exit(0)
    except Exception as e:
        print(json.dumps({"event": "dataset.export_failed", **mdc, "error": str(e)}))
        sys.exit(1)


if __name__ == "__main__":
    main()
