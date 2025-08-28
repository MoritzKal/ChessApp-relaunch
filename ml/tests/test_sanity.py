import importlib
import os
import subprocess
import sys


def test_python_env_and_import_path():
    # Basic environment checks
    assert sys.version_info.major >= 3

    # Ensure project root is on sys.path
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
    if repo_root not in sys.path:
        sys.path.insert(0, repo_root)

    # Try a trivial import (skip if not present)
    try:
        importlib.import_module('ml')
    except ModuleNotFoundError:
        # acceptable if ml is not a package
        pass


def test_train_help_if_present():
    # If a train.py exists, ensure it has --help
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
    train_py = os.path.join(repo_root, 'ml', 'train.py')
    if not os.path.exists(train_py):
        return  # skip
    try:
        out = subprocess.check_output([sys.executable, train_py, '--help'], stderr=subprocess.STDOUT, timeout=10)
        assert b'--help' in out or len(out) > 0
    except Exception as e:
        # Fail with output for context
        raise AssertionError(f"train.py --help failed: {e}")

