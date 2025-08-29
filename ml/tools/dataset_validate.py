#!/usr/bin/env python3
import argparse, json, sys, os, glob, csv, time, uuid
from typing import List, Dict, Any
import pandas as pd
import chess

REQUIRED = ["game_id", "ply", "fen", "uci", "color", "result"]

def read_rows(path: str) -> pd.DataFrame:
    files = sorted(glob.glob(os.path.join(path, "*.jsonl")) + glob.glob(os.path.join(path, "*.ndjson")))
    if not files:
        raise FileNotFoundError(f"No *.jsonl found under {path}")
    dfs = [pd.read_json(f, lines=True) for f in files]
    return pd.concat(dfs, ignore_index=True)

def validate(df: pd.DataFrame) -> Dict[str, Any]:
    invalid_rows = []
    reasons_count: Dict[str, int] = {}
    total = len(df)
    seen = set()
    for idx, row in df.iterrows():
        reason = None
        # required fields
        for f in REQUIRED:
            if pd.isna(row.get(f)):
                reason = "missing_field"; break
        if not reason:
            # duplicates key: game_id+ply
            key = (row.get("game_id"), int(row.get("ply", -1)))
            if key in seen:
                reason = "duplicate_ply"
            else:
                seen.add(key)
        if not reason:
            # FEN valid
            fen = row.get("fen")
            try:
                board = chess.Board(fen)
            except Exception:
                reason = "invalid_fen"
        if not reason:
            # UCI legal
            uci = row.get("uci")
            try:
                move = chess.Move.from_uci(str(uci))
                if move not in board.legal_moves:
                    reason = "uci_not_legal"
            except Exception:
                reason = "uci_parse_error"
        if reason:
            reasons_count[reason] = reasons_count.get(reason, 0) + 1
            invalid_rows.append({"idx": int(idx), "reason": reason, **{k: row.get(k, None) for k in ["game_id","ply","fen","uci","color"]}})
    return {
        "rows_total": total,
        "invalid_rows_total": len(invalid_rows),
        "invalid_by_reason": reasons_count,
        "invalid_rows_sample": invalid_rows[:50],
    }

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--path", required=True, help="Directory with *.jsonl dataset rows")
    ap.add_argument("--report", default="validation_report.json")
    ap.add_argument("--invalid-sample", default="invalid_rows_sample.csv")
    ap.add_argument("--dataset-id", required=True)
    ap.add_argument("--run-id", default=None)
    args = ap.parse_args()

    run_id = args.run_id or f"local-{uuid.uuid4().hex[:8]}"
    mdc = {
        "dataset_id": args.dataset_id,
        "run_id": run_id,
        "component": "ml.tools.dataset_validate",
        "username": os.environ.get("USER", "local"),
    }
    print(json.dumps({"event": "dataset.validate_started", **mdc}))

    try:
        t0 = time.time()
        df = read_rows(args.path)
        report = validate(df)
        report.update({
            "dataset_id": args.dataset_id,
            "run_id": run_id,
            "elapsed_ms": int((time.time() - t0) * 1000),
        })

        with open(args.report, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        if report["invalid_rows_total"] > 0:
            with open(args.invalid_sample, "w", newline="", encoding="utf-8") as f:
                w = csv.DictWriter(f, fieldnames=["idx","reason","game_id","ply","fen","uci","color"])
                w.writeheader()
                for r in report["invalid_rows_sample"]:
                    w.writerow(r)

        log = {
            "event": "dataset.validate_completed",
            **mdc,
            "rows_total": report["rows_total"],
            "invalid_rows_total": report["invalid_rows_total"],
            "invalid_by_reason": report["invalid_by_reason"],
            "elapsed_ms": report["elapsed_ms"],
        }
        print(json.dumps(log))
        sys.exit(0)
    except Exception as e:
        print(json.dumps({"event": "dataset.validate_failed", **mdc, "error": str(e)}))
        sys.exit(1)

if __name__ == "__main__":
    main()
