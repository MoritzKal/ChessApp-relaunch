#!/usr/bin/env python3
# chs_* Metriken aus dem Code ermitteln
import re, pathlib
SRC = pathlib.Path(".")
mets = set()

for p in SRC.rglob("*.*"):
    if p.suffix.lower() in [".md",".png",".jpg",".jpeg",".gif",".lock",".svg",".ico"]:
        continue
    try:
        s = p.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        continue
    for m in re.finditer(r'chs_[a-zA-Z0-9_]+', s):
        mets.add(m.group(0))

print("# Discovered Metrics\n")
for m in sorted(mets):
    print(f"- {m}")
