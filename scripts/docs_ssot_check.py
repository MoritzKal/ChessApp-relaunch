#!/usr/bin/env python3
import re, sys, pathlib
ROOT = pathlib.Path(__file__).resolve().parents[1] / "docs"

# Minimaler SSOT-Check: API-Details dürfen NICHT im OVERVIEW/README landen.
RULES = [
    (ROOT / "PROJECT_OVERVIEW.md", r"/v1/"),
]

errors = []
for path, pattern in RULES:
    if path.exists():
        txt = path.read_text(encoding="utf-8", errors="ignore")
        if re.search(pattern, txt):
            errors.append(f"SSOT violation in {path.name}: pattern {pattern}")

if errors:
    print("\n".join(errors))
    sys.exit(1)

print("SSOT OK")
