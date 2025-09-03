import re, yaml, pathlib, sys

root = pathlib.Path("reports/diff")
A = yaml.safe_load((root/"effective.A.yml").read_text())
C = yaml.safe_load((root/"effective.current.yml").read_text())

def extract(y):
    svcs = (y or {}).get("services", {}) or {}
    out = {}
    for n, s in svcs.items():
        env = s.get("environment", {}) or {}
        if isinstance(env, list):
            env = {k.split('=')[0]: None for k in env}
        env_keys = [k for k in env.keys() if re.match(r'^(SPRING_|APP_SECURITY_|POSTGRES_|MINIO_)', k or '')]
        ports = [str(p) for p in (s.get("ports") or [])]
        deps  = s.get("depends_on") or []
        if isinstance(deps, dict): deps = list(deps.keys())
        hchk  = list((s.get("healthcheck") or {}).keys())
        out[n] = dict(env_keys=sorted(env_keys), ports=ports, depends_on=sorted(deps), healthcheck=sorted(hchk))
    return out

EA, EC = extract(A), extract(C)
services = sorted(set(EA)|set(EC))

def diff_list(a,b): 
    a,b = a or [], b or []
    return [("+"+x) for x in b if x not in a] + [("-"+x) for x in a if x not in b]

def bp_env(keys):
    bp=[]
    if any(k.startswith("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_") for k in keys):
        bp.append("JWT via SPRING_SECURITY_* (Konflikt-Risiko mit APP_SECURITY_*)")
    if any(k=="APP_SECURITY_JWT_SECRET" for k in keys):
        bp.append("Eigener JwtDecoder-Secret (APP_SECURITY_*)  Konsistenz prüfen")
    return bp

lines=["# Diff-Matrix: Pipeline A (46dd899) vs Current (dev-fix)\n"]
for svc in services:
    a,b = EA.get(svc,{}), EC.get(svc,{})
    if not a and b: lines.append(f"## Service **{svc}** (neu)")
    elif a and not b: lines.append(f"## Service **{svc}** (entfernt)")
    else: lines.append(f"## Service **{svc}**")
    for key in ("ports","depends_on","healthcheck","env_keys"):
        dl = diff_list(a.get(key,[]), b.get(key,[]))
        if dl:
            lines.append(f"- **{key}**: " + ", ".join(dl))
    bp = set(bp_env(a.get("env_keys",[])) + bp_env(b.get("env_keys",[])))
    if bp: lines.append("> Breaking potential: " + " | ".join(sorted(bp)))
    lines.append("")

# Anhang: Roh-Diffs
lines.append("\n---\n### Git-Diffs (ausgewählte Pfade)\n")
try:
    gd = pathlib.Path("reports/diff/git_diffs.patch").read_text()
except FileNotFoundError:
    gd = "(keine git_diffs.patch gefunden)"
lines.append("```diff\n"+gd+"\n```")

print("\n".join(lines))
