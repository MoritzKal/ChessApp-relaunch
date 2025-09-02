"""Prometheus metrics for the self-play runner."""
from prometheus_client import Counter, Gauge, Histogram

# Counter for game results
games_total = Counter(
     "chs_selfplay_games_total",
     "Number of self-play games played",
     labelnames=["result"],
 )

# Gauge for current Elo estimate
elo_estimate = Gauge(
    "chs_selfplay_elo_estimate", "Estimated Elo difference between models"
)

# Histogram for move time
move_time_seconds = Histogram(
    "chs_selfplay_move_time_seconds",
    "Time spent generating a move",
    buckets=[0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2],
)

# Gauge for queue depth
queue_depth = Gauge(
    "chs_selfplay_queue_depth", "Number of outstanding games in the queue"
)

# Counter for errors
errors_total = Counter(
    "chs_selfplay_errors_total", "Errors when calling serve", labelnames=["type"]
)
