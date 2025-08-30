from fastapi import APIRouter, BackgroundTasks, HTTPException
from pydantic import BaseModel
import importlib, inspect, asyncio, os, time, pathlib, re
from types import SimpleNamespace
from typing import Any

router = APIRouter()

class SelfPlayReq(BaseModel):
    n_games: int = 20
    policy: str = "baseline"
    run_id: str | None = None
    username: str | None = None
    sync: bool = False

# ---- Helpers ---------------------------------------------------------------
def _as_int(x: Any, default: int) -> int:
    try:
        return int(x)
    except Exception:
        return int(default)

def _ensure_dirs(*paths: str):
    import pathlib
    for p in paths:
        pathlib.Path(p).mkdir(parents=True, exist_ok=True)

def _import_first(*modnames: str):
    last_err = None
    for m in modnames:
        try:
            return importlib.import_module(m)
        except Exception as e:
            last_err = e
            continue
    raise last_err or ImportError(f"none of modules present: {modnames!r}")

def _load_runner_module():
    # Beide Varianten probieren (Projektstruktur schwankt häufig zwischen app.* und app.app.*)
    return _import_first("app.selfplay.runner", "app.app.selfplay.runner")

def _load_config_module():
    # Mögliche Config-Orte abklopfen (falls vorhanden)
    try:
        return _import_first(
            "app.selfplay.config", "app.app.selfplay.config",
            "app.selfplay.conf",   "app.app.selfplay.conf",
        )
    except Exception:
        return None

def _build_cfg(n_games: int, policy: str, run_id: str | None, username: str | None):
    """
    Versucht in dieser Reihenfolge:
    1) Factory-Funktion im Config- oder Runner-Modul: default_cfg()/make_cfg()/build_cfg(...)
    2) Dataclass/Config-Klasse: SelfPlayConfig/Config mit sinnvollen Defaults
    3) Fallback: SimpleNamespace mit üblichen Feldern
    """
    # 1) Factories in möglichen Modulen
    factories = ("default_cfg", "make_cfg", "build_cfg")
    cfg_mods = [m for m in (_load_config_module(), _load_runner_module()) if m]
    for mod in cfg_mods:
        for name, obj in inspect.getmembers(mod):
            if name in factories and inspect.isfunction(obj):
                try:
                    # Häufige Parameter-Sets probieren
                    for kwargs in (
                        dict(n_games=n_games, policy=policy, run_id=run_id, username=username),
                        dict(n_games=n_games, policy=policy, run_id=run_id),
                        dict(n_games=n_games, policy=policy),
                        dict(),
                    ):
                        try:
                            cfg = obj(**kwargs)
                            # Setze zumindest diese Felder sicher
                            for k, v in dict(n_games=n_games, policy=policy,
                                             run_id=run_id, username=username).items():
                                if v is not None and not hasattr(cfg, k):
                                    setattr(cfg, k, v)
                            return cfg
                        except TypeError:
                            continue
                except Exception:
                    continue  # nächste Option testen

    # 2) Dataclass/Config-Klassen
    for mod in cfg_mods:
        for name, obj in inspect.getmembers(mod):
            if name.lower() in ("selfplayconfig", "config") and inspect.isclass(obj):
                try:
                    try:
                        cfg = obj()  # Defaults
                    except TypeError:
                        # Minimal-Constructor
                        cfg = obj(n_games=n_games, policy=policy)
                    # Felder auf jeden Fall setzen/überschreiben
                    for k, v in dict(n_games=n_games, policy=policy,
                                     run_id=run_id, username=username).items():
                        if v is not None:
                            setattr(cfg, k, v)
                    return cfg
                except Exception:
                    continue

    # 3) Fallback – SimpleNamespace mit typischen Feldern  Verzeichnisse
    rid = run_id or f"sp-{int(time.time())}"
    base = os.getenv("CHS_DATA_DIR", "/data")
    out_dir = os.path.join(base, "selfplay", rid)
    log_dir = os.path.join(out_dir, "logs")
    replay_dir = os.path.join(out_dir, "replays")
    _ensure_dirs(log_dir, replay_dir)
    pathlib.Path(log_dir).mkdir(parents=True, exist_ok=True)
    pathlib.Path(replay_dir).mkdir(parents=True, exist_ok=True)
    cfg = SimpleNamespace(
        # Kernparameter
        n_games=_as_int(n_games, 10),
        policy=policy,
        run_id=rid,
        username=username or "dev",
        # häufige Default-Felder
        seed=42,
        device="cpu",
        max_moves=512,
        temperature=0.0,
        # Verzeichnisse
        out_dir=out_dir,
        log_dir=log_dir,
        replay_dir=replay_dir,
        write_replays=False,
    )
    cfg.games      = getattr(cfg, "games", None)      or cfg.n_games
    cfg.num_games  = getattr(cfg, "num_games", None)  or cfg.n_games

    for name, default in [
        ("epochs", 1),
        ("steps", 1),
        ("steps_per_epoch", 1),
        ("num_workers", 1),
        ("threads", 1),
        ("mcts_simulations", 16),
        ("playouts", 16),
        ("batch_size", 1),
    ]:
        val = getattr(cfg, name, None)
        if val is None:
            setattr(cfg, name, default)
        else:
            setattr(cfg, name, _as_int(val, default))
    return cfg


def _call_runner(n_games: int, policy: str, run_id: str | None, username: str | None):
    mod = _load_runner_module()
    # Runner-Funktion finden
    candidates = []
    for name, obj in inspect.getmembers(mod, inspect.isfunction):
        low = name.lower()
        if any(k in low for k in ("selfplay_loop", "selfplay", "self_play", "arena", "run")):
            candidates.append(obj)
    if not candidates:
        raise HTTPException(status_code=501, detail="No self-play function found in runner module")

    # Wenn Signatur 'cfg' verlangt, bauen wir cfg – sonst versuchen wir frühere Modi
    last_err = None
    for fn in candidates:
        sig = inspect.signature(fn)
        params = list(sig.parameters.values())
        # Fall 1: exakter cfg-Parameter
        if len(params) >= 1 and params[0].name in ("cfg", "config", "cfg_"):
            cfg = _build_cfg(n_games, policy, run_id, username)
            # Auto-Heilung: falls AttributeError "cfg.<name> fehlt", setze sinnvollen Default und wiederhole
            for attempt in range(6):
                try:
                    for name in ("n_games","games","num_games","epochs","steps","steps_per_epoch",
                                  "num_workers","threads","mcts_simulations","playouts","batch_size","max_moves"):
                        if hasattr(cfg, name):
                            setattr(cfg, name, _as_int(getattr(cfg, name), getattr(cfg, name) or 1))
                    res = fn(cfg)
                    if inspect.iscoroutine(res):
                        res = asyncio.run(res)
                    return {"ok": True, "runner": fn.__name__, "called_with": "cfg", "attempts": attempt+1, "return": str(res)}
                except AttributeError as e:
                    msg = str(e)
                    m = re.search(r"object has no attribute '([^']+)'", msg)
                    if not m:
                        raise HTTPException(status_code=500, detail=f"Runner {fn.__name__} failed with cfg: {e}")
                    missing = m.group(1)
                    # Heuristiken für häufige Felder
                    if "dir" in missing:
                        base = os.getenv("CHS_DATA_DIR", "/data")
                        rid = getattr(cfg, "run_id", run_id) or f"sp-{int(time.time())}"
                        val = os.path.join(base, "selfplay", rid, missing.replace("_dir",""))
                        pathlib.Path(val).mkdir(parents=True, exist_ok=True)
                        setattr(cfg, missing, val)
                    elif missing in ("seed", "max_moves"):
                        setattr(cfg, missing, 42 if missing=="seed" else 512)
                    elif missing in ("device",):
                        setattr(cfg, missing, "cpu")
                    elif missing in ("policy",):
                        setattr(cfg, missing, policy)
                    elif missing in ("run_id",):
                        setattr(cfg, missing, getattr(cfg, "run_id", run_id) or f"sp-{int(time.time())}")
                    elif missing in ("username",):
                        setattr(cfg, missing, username or "dev")
                    else:
                        # generischer, sicherer Default
                        setattr(cfg, missing, None)
                    continue
                except Exception as e:
                    raise HTTPException(status_code=500, detail=f"Runner {fn.__name__} failed with cfg: {e}")
        # Fall 2: funktionsbasierte Aufrufe ohne cfg (ältere Varianten)
        for kwargs in (
            dict(n_games=n_games, policy=policy, run_id=run_id, username=username),
            dict(n_games=n_games, policy=policy, run_id=run_id),
            dict(n_games=n_games, policy=policy),
            dict(games=n_games, policy=policy),
            dict(n=n_games),
            dict(),
        ):
            try:
                res = fn(**kwargs)
                if inspect.iscoroutine(res):
                    res = asyncio.run(res)
                return {"ok": True, "runner": fn.__name__, "called_with": "kwargs", "kwargs": kwargs, "return": str(res)}
            except TypeError as e:
                last_err = e
                continue
            except Exception as e:
                raise HTTPException(status_code=500, detail=f"Runner {fn.__name__} failed: {e}")

    raise HTTPException(status_code=400, detail=f"No compatible signature worked. Last error: {last_err}")

# ---- Route -----------------------------------------------------------------

@router.post("/selfplay/run")
def selfplay_run(req: SelfPlayReq, bg: BackgroundTasks):
    if req.sync:
        return _call_runner(req.n_games, req.policy, req.run_id, req.username)
    bg.add_task(_call_runner, req.n_games, req.policy, req.run_id, req.username)
    return {"accepted": True, "n_games": req.n_games, "policy": req.policy, "run_id": req.run_id}
