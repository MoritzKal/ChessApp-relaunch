import os
import urllib.request
import urllib.error
import pytest

GRAFANA_URL = os.getenv("GRAFANA_URL", "http://localhost:3000")
LOKI_URL = os.getenv("LOKI_URL", "http://localhost:3100")
PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://localhost:9090")


def service_up(url, path=""):
    try:
        with urllib.request.urlopen(url + path, timeout=2):
            return True
    except Exception:
        return False


@pytest.mark.skipif(not service_up(LOKI_URL, "/ready"), reason="Loki not reachable")
def test_loki_ready():
    with urllib.request.urlopen(f"{LOKI_URL}/ready", timeout=5) as resp:
        assert resp.status == 200


@pytest.mark.skipif(not service_up(GRAFANA_URL, "/api/health"), reason="Grafana not reachable")
def test_grafana_has_loki_datasource():
    user = os.getenv("GRAFANA_USER", "admin")
    password = os.getenv("GRAFANA_PASSWORD", "admin")
    password_mgr = urllib.request.HTTPPasswordMgrWithDefaultRealm()
    password_mgr.add_password(None, GRAFANA_URL, user, password)
    handler = urllib.request.HTTPBasicAuthHandler(password_mgr)
    opener = urllib.request.build_opener(handler)
    with opener.open(f"{GRAFANA_URL}/api/datasources/name/Loki", timeout=5) as resp:
        assert resp.status == 200


@pytest.mark.skipif(not service_up(PROMETHEUS_URL, "/-/ready"), reason="Prometheus not reachable")
def test_prometheus_ready():
    with urllib.request.urlopen(f"{PROMETHEUS_URL}/-/ready", timeout=5) as resp:
        assert resp.status == 200
