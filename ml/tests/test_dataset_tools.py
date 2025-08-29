import json, os, tempfile, pandas as pd, subprocess, sys, pathlib

def _cli(script, args):
    env = os.environ.copy()
    env["PYTHONPATH"] = str(pathlib.Path(__file__).resolve().parents[2])
    return subprocess.run([sys.executable, script] + args, env=env, capture_output=True, text=True)

def test_validator_ok(tmp_path):
    # create sample jsonl
    data = [
        {"game_id":"g1","ply":1,"fen":"startpos fen not used","uci":"e2e4","color":"white","result":"white"}
    ]
    # Use a legal fen
    data[0]["fen"] = "rn1qkbnr/ppp1pppp/8/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R w KQkq - 1 3"
    p = tmp_path/"sample.jsonl"
    with open(p,"w") as f:
        for r in data:
            f.write(json.dumps(r)+"\n")
    out = _cli(str(pathlib.Path(__file__).resolve().parents[1]/"tools"/"dataset_validate.py"),
               ["--path", str(tmp_path), "--dataset-id","testds", "--report", str(tmp_path/"report.json")])
    assert out.returncode == 0

def test_export_creates_manifest(tmp_path):
    # prepare minimal dataset
    rows = [
        {"game_id":"g1","ply":1,"fen":"rn1qkbnr/ppp1pppp/8/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R w KQkq - 1 3","uci":"e2e4","color":"white","result":"white","time_category":"blitz","white_rating":1100,"black_rating":1050},
        {"game_id":"g1","ply":2,"fen":"rn1qkbnr/ppp1pppp/8/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R b KQkq - 1 3","uci":"d7d5","color":"black","result":"white","time_category":"blitz","white_rating":1100,"black_rating":1050},
    ]
    p = tmp_path/"sample.jsonl"
    with open(p,"w") as f:
        for r in rows:
            f.write(json.dumps(r)+"\n")
    out = _cli(str(pathlib.Path(__file__).resolve().parents[1]/"tools"/"dataset_export.py"),
               ["--input", str(tmp_path), "--output", str(tmp_path/"out.parquet"), "--dataset-id","testds", "--manifest", str(tmp_path/"manifest.json")])
    assert out.returncode == 0
    assert (tmp_path/"manifest.json").exists()
