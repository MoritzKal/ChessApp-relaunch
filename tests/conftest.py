import sys
import types
from pathlib import Path
import importlib.machinery

# ensure ml directory on path
root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / 'ml'))

# expose selfplay_runner package alias for hyphenated directory
spr_root = root / 'ml' / 'selfplay-runner'
pkg = types.ModuleType('selfplay_runner')
loader_spec = importlib.machinery.ModuleSpec('selfplay_runner', loader=None, is_package=True)
loader_spec.submodule_search_locations = [str(spr_root)]
pkg.__spec__ = loader_spec
pkg.__path__ = [str(spr_root)]
sys.modules.setdefault('selfplay_runner', pkg)

# minimal tenacity stub with retry support
class stop_after_attempt:
    def __init__(self, attempts: int):
        self.attempts = attempts

class wait_exponential:
    def __init__(self, **kwargs):
        pass

def retry(stop=None, wait=None):
    def decorator(fn):
        def wrapper(*args, **kwargs):
            max_attempts = getattr(stop, 'attempts', 1)
            attempt = 0
            while True:
                try:
                    return fn(*args, **kwargs)
                except Exception:
                    attempt += 1
                    if attempt >= max_attempts:
                        raise
        return wrapper
    return decorator

tn = types.ModuleType('tenacity')
tn.retry = retry
tn.stop_after_attempt = stop_after_attempt
tn.wait_exponential = wait_exponential
sys.modules.setdefault('tenacity', tn)
