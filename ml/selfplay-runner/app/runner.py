"""Self-play run orchestration."""
from __future__ import annotations

import random
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List
from uuid import uuid4

import chess

from . import metrics, storage, elo
from .schemas import RunStatus, StartRequest
from .serve_client import ServeClient


@dataclass
class RunInfo:
    request: StartRequest
    status: str = "running"
    progress: Dict[str, int] = field(default_factory=lambda: {"played": 0, "total": 0})
    metrics: Dict[str, float | None] = field(
        default_factory=lambda: {"elo": None, "winRate": None}
    )
    results: List[dict] = field(default_factory=list)
    started_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    finished_at: str | None = None
    report_uri: str | None = None


class SelfPlayRunner:
    """Coordinates self-play runs."""

    def __init__(self, client: ServeClient) -> None:
        self.client = client
        self.runs: Dict[str, RunInfo] = {}
        self._lock = threading.Lock()

    def start(self, request: StartRequest) -> str:
        run_id = uuid4().hex
        info = RunInfo(request=request)
        info.progress["total"] = request.games
        with self._lock:
            self.runs[run_id] = info
        threading.Thread(target=self._run, args=(run_id,), daemon=True).start()
        return run_id

    def get_status(self, run_id: str) -> RunStatus | None:
        with self._lock:
            info = self.runs.get(run_id)
        if not info:
            return None
        return RunStatus(
            runId=run_id,
            status=info.status,
            progress=info.progress,
            metrics=info.metrics,
            reportUri=info.report_uri,
        )

    # internal
    def _run(self, run_id: str) -> None:
        info = self.runs[run_id]
        req = info.request
        wins = losses = draws = 0
        counter_lock = threading.Lock()
        random.seed(req.seed)
        metrics.queue_depth.set(req.games)

        def play_game(game_idx: int) -> None:
            nonlocal wins, losses, draws
            board = chess.Board()
            white_model = req.modelId if game_idx % 2 == 0 else req.baselineId
            black_model = req.baselineId if game_idx % 2 == 0 else req.modelId
            while not board.is_game_over():
                model = white_model if board.turn == chess.WHITE else black_model
                fen = board.fen()
                start_t = time.perf_counter()
                try:
                    move_str = self.client.predict(fen, model)
                    move = chess.Move.from_uci(move_str)
                    if move not in board.legal_moves:
                        raise ValueError("illegal move")
                except Exception:
                    move = random.choice(list(board.legal_moves))
                duration = time.perf_counter() - start_t
                metrics.move_time_seconds.observe(duration)
                board.push(move)
            result = board.result()
            if result == "1-0":
                if white_model == req.modelId:
                    res = "win"
                    dw, dl, dd = 1, 0, 0
                else:
                    res = "loss"
                    dw, dl, dd = 0, 1, 0
            elif result == "0-1":
                if black_model == req.modelId:
                    res = "win"
                    dw, dl, dd = 1, 0, 0
                else:
                    res = "loss"
                    dw, dl, dd = 0, 1, 0
            else:
                res = "draw"
                dw, dl, dd = 0, 0, 1
            with counter_lock:
                wins += dw
                losses += dl
                draws += dd
            metrics.games_total.labels(result=res).inc()
            info.results.append(
                {"gameIdx": game_idx, "result": res, "plyCount": board.ply()}
            )
            info.progress["played"] += 1
            metrics.queue_depth.set(req.games - info.progress["played"])

        with ThreadPoolExecutor(max_workers=req.concurrency) as ex:
            futures = [ex.submit(play_game, i) for i in range(req.games)]
            for _ in as_completed(futures):
                pass

        info.finished_at = datetime.utcnow().isoformat()
        wins_s = wins + 0.5
        losses_s = losses + 0.5
        p = (wins_s + 0.5 * draws) / (wins_s + losses_s + draws)
        info.metrics["winRate"] = p
        elo_val = elo.win_rate_to_elo(p)
        info.metrics["elo"] = elo_val
        metrics.elo_estimate.set(elo_val)
        report = {
            "runId": run_id,
            "request": req.model_dump(),
            "startedAt": info.started_at,
            "finishedAt": info.finished_at,
            "results": info.results,
            "summary": {"winRate": p, "elo": elo_val},
        }
        info.report_uri = storage.save_report(run_id, report)
        info.status = "completed"
        metrics.queue_depth.set(0)

    # for debug endpoint
    def run_debug(self, games: int) -> RunStatus:
        req = StartRequest(
            modelId="debug", baselineId="debug", games=games, concurrency=1, seed=42
        )
        run_id = self.start(req)
        # poll until done
        while True:
            status = self.get_status(run_id)
            if status and status.status == "completed":
                return status
            time.sleep(0.1)
