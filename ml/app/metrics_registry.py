from prometheus_client import CollectorRegistry, Counter, Gauge

REGISTRY = CollectorRegistry()
_LABELS = ["run_id", "policy", "username", "component"]

chs_selfplay_runs_active = Gauge("chs_selfplay_runs_active",
    "Active self-play runs", _LABELS, registry=REGISTRY)
chs_selfplay_games_total = Counter("chs_selfplay_games_total",
    "Self-play games played", _LABELS, registry=REGISTRY)
chs_selfplay_moves_total = Counter("chs_selfplay_moves_total",
    "Self-play moves played", _LABELS, registry=REGISTRY)
chs_selfplay_win_rate = Gauge("chs_selfplay_win_rate",
    "White win rate (0..1)", _LABELS, registry=REGISTRY)
chs_selfplay_dataset_rows_total = Counter("chs_selfplay_dataset_rows_total",
    "Rows written to dataset", _LABELS, registry=REGISTRY)
chs_selfplay_failures_total = Counter("chs_selfplay_failures_total",
    "Self-play failures", _LABELS, registry=REGISTRY)

def labelset(run_id: str, policy: str, username: str, component: str = "ml"):
    return dict(run_id=run_id, policy=policy, username=username, component=component)
