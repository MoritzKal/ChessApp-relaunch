import os
import re
import urllib.request
import pytest


@pytest.mark.integration
def test_metrics_names_present():
    if not os.environ.get("CHS_RUN_SCRAPE_TEST"):
        pytest.skip("Set CHS_RUN_SCRAPE_TEST=1 to hit http://localhost:8000/metrics")
    url = os.environ.get("CHS_METRICS_URL", "http://localhost:8000/metrics")
    with urllib.request.urlopen(url, timeout=3) as r:
        body = r.read().decode("utf-8", "ignore")
    for name in [
        "chs_selfplay_runs_active",
        "chs_selfplay_games_total",
        "chs_selfplay_moves_total",
        "chs_selfplay_win_rate",
        "chs_selfplay_dataset_rows_total",
        "chs_selfplay_failures_total",
    ]:
        assert re.search(rf'^{name}{{', body, flags=re.M), f"{name} missing"
