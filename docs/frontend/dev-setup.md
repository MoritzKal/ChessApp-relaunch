# Dev-Setup (npm)
- Node 20+, npm 10+
- `.env.development`:
  VITE_API_BASE=http://localhost:8080
  VITE_OBS_BASE=http://localhost:3000
  VITE_DEV_STATIC_TOKEN=dev-abc123
- Start: `npm ci && npm run dev` (Vite, Hot Reload)
- Vite-Proxy (vite.config.ts):
  server: { proxy: { "/v1": VITE_API_BASE, "/actuator": VITE_API_BASE } }
