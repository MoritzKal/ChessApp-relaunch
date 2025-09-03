# Docker/Compose (Prod & Dev)
**Prod:** Multi-Stage Build (Node 20-alpine → nginx:alpine)
- `frontend/Dockerfile` (build → /usr/share/nginx/html)
- `frontend/nginx.conf` (SPA-Fallback auf /index.html)
**Dev (optional):** `frontend/Dockerfile.dev` (Vite in Container, Bind-Mount)
**Compose-Erweiterung:** infra/docker-compose.yml → Service `frontend` (Prod) + `fe-dev` (Dev)
Ports: 5173 (dev), 8081 (prod nginx). ENV: API_BASE via `VITE_API_BASE`.
