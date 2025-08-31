# Runbook: Local Smoke Test

## Prerequisites
- Docker Compose, curl and bash available
- Repository cloned with `smoke_chessapp.sh`

## Steps
1. Start all services:
   ```bash
   make up
   ```
2. Make the script executable and run it:
   ```bash
   chmod +x smoke_chessapp.sh
   ./smoke_chessapp.sh
   ```
3. Review the summary at the end; all checks should report `ok`.

## Troubleshooting
- Check container logs with `docker compose logs <svc> --tail=200`.
- Ensure the expected ports (8080, 9090, 3000, 3100) are free.
