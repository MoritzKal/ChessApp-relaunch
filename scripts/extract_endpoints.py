#!/usr/bin/env python3
# Heuristisch Endpunkte aus Source scannen & Tabelle ausgeben
import re, pathlib

SRC = pathlib.Path(".")
routes = set()

for p in SRC.rglob("*.*"):
    if p.suffix.lower() not in [".java",".kt",".ts",".py",".go",".rb",".cs",".js"]:
        continue
    try:
        s = p.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        continue
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping\("([^"]+)"\)', s):
        routes.add((m.group(1).upper(), m.group(2), p.as_posix()))
    for m in re.finditer(r'(GET|POST|PUT|DELETE|PATCH)\s+[\'"](/[^\'"]+)[\'"]', s):
        routes.add((m.group(1).upper(), m.group(2), p.as_posix()))

print("# Discovered Endpoints\n\n| Method | Path | File |\n|---|---|---|")
for method, path, file in sorted(routes):
    print(f"| {method} | {path} | {file} |")
