import json, time
import chess
from fastapi.testclient import TestClient
from ml.serve.app import app

client = TestClient(app)

FENS = [
    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
    "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
    "r1bqk1nr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 3 4",
    "rnbqkbnr/ppp1pppp/8/3p4/2P5/8/PP1PPPPP/RNBQKBNR w KQkq - 0 2",
    "8/8/8/8/4k3/8/4K3/8 w - - 0 1",
    "r2q1rk1/pp1nbppp/2np1n2/2p1p3/2P1P3/1PNPB1P1/PB1N1PBP/R2Q1RK1 w - - 0 10",
    "r3k2r/pppq1ppp/2n1bn2/3p4/3P4/2N1PN2/PPPQ1PPP/R3K2R w KQkq - 0 10",
    "r1bq1rk1/ppp2ppp/2n2n2/3pp3/1b1PP3/2N2N2/PPP1BPPP/R1BQ1RK1 w - - 0 7",
]

def test_predict_returns_legal_moves_for_smoke_set():
    for fen in FENS:
        r = client.post("/predict", json={"fen": fen})
        assert r.status_code == 200, r.text
        data = r.json()
        assert data["legal"] is True
        mv = data["move"]
        board = chess.Board(fen)
        legal_uci = {m.uci() for m in board.legal_moves}
        assert mv in legal_uci
        assert data["model_id"]
        assert data["model_version"]
        assert isinstance(data["policy"], list) and len(data["policy"]) >= 1

def test_p95_latency_under_150ms_local():
    durations = []
    for i in range(200):
        fen = FENS[i % len(FENS)]
        t0 = time.perf_counter()
        r = client.post("/predict", json={"fen": fen})
        assert r.status_code == 200
        durations.append(time.perf_counter() - t0)
    durations.sort()
    p95 = durations[int(0.95 * len(durations)) - 1]
    assert p95 <= 0.150, f"p95 too high: {p95*1000:.1f}ms"
