from prometheus_client import Counter, Histogram

chs_selfplay_games_total = Counter(
    "chs_selfplay_games_total", "Self-Play games", ["result", "run_id"]
)
chs_selfplay_wins_total = Counter(
    "chs_selfplay_wins_total", "Self-Play wins", ["run_id"]
)
chs_selfplay_failures_total = Counter(
    "chs_selfplay_failures_total", "Self-Play failures", ["type", "run_id"]
)
chs_dataset_rows = Counter(
    "chs_dataset_rows", "Dataset rows", ["dataset_id"]
)
chs_dataset_invalid_rows_total = Counter(
    "chs_dataset_invalid_rows_total", "Invalid dataset rows", ["dataset_id"]
)
chs_dataset_export_duration_seconds = Histogram(
    "chs_dataset_export_duration_seconds", "Export duration"
)
