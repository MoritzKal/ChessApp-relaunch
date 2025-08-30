# Windows Git-Bash: Docker Path Rewriting (MSYS)

Problem: In Git-Bash, MSYS performs automatic path conversion. Absolute UNIX-like paths such as `/app/ml/...` are rewritten to Windows paths like `C:/Program Files/Git/app/ml/...` when arguments are passed to Windows binaries (e.g., `docker.exe`). This breaks container commands like `docker compose exec ml python ... --input /app/ml/data/...`.

Fix: Disable path conversion for docker commands in this shell session and wrap the container command in a shell on the container side to avoid further rewrites.

- Set environment variables before docker commands:
  - `export MSYS_NO_PATHCONV=1`
  - `export MSYS2_ARG_CONV_EXCL="*"`

- Prefer a shell-wrapped exec to isolate arguments inside the container:

```
docker compose -f infra/docker-compose.yml exec -T ml sh -lc 'python ml/tools/dataset_export.py \
  --input /app/ml/data/sample \
  --output /app/ml/out/a2/compact.parquet \
  --dataset-id "ds_v0_local" \
  --manifest /app/ml/out/a2/manifest.json \
  --push-metrics http://ml:8000/internal/dataset/metrics'
```

Notes:
- The provided script `scripts/a2_verify_min.sh` already exports these variables and uses the shell-wrapped exec. Use it directly from Git-Bash.
- The same pattern works with both `docker compose` (v2) and `docker-compose` (v1).

