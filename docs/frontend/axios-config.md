# Axios Config (Vite + Vue)

## Basis
- Base URL: env `VITE_API_BASE_URL`
- Timeout: 10s
- Headers: `Authorization: Bearer <jwt>`, `X-Correlation-Id: <uuid>`

## Interceptors
- **Request:** fügt Token & Correlation-Id hinzu
- **Response:** mappt Fehler → UI-Fehlerdomäne; extrahiert `Location`

## Retries
- GET (network/5xx) max 2, exponential backoff (250ms, 1s)

## Telemetrie-Hooks (optional)
- Timing via `performance.now()` → POST `/v1/metrics/ui` (falls aktiviert)

## CORS
- DEV permissiv; PROD via Reverse Proxy/API-GW härten
- Wenn die UI per Interceptor `X-Correlation-Id` setzt, muss dieser Header serverseitig erlaubt werden. Beispiel (Spring Security CORS):
  ```java
  conf.setAllowedHeaders(List.of(
      "Authorization",
      "Content-Type",
      "X-Requested-With",
      "X-Debug-User",
      "X-Correlation-Id"
  ));
  conf.setExposedHeaders(List.of("Location", "X-Request-Id"));
  ```
  Bei Preflight-Fehlern wie „Request header field x-correlation-id is not allowed by Access-Control-Allow-Headers“ sicherstellen, dass der Header exakt (Groß-/Kleinschreibung egal, aber konsistent) aufgeführt ist und `OPTIONS` als Methode erlaubt ist.

## Beispiel
```ts
import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('chs_jwt');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  cfg.headers['X-Correlation-Id'] = crypto.randomUUID();
  return cfg;
});

api.interceptors.response.use(
  r => r,
  err => {
    // Normalize error
    const res = err.response;
    const norm = {
      ok: false,
      status: res?.status ?? 0,
      code: res?.data?.code || (res?.status === 401 ? 'UNAUTHORIZED' : 'INTERNAL'),
      message: res?.data?.message || err.message,
      details: res?.data?.errors || {}
    };
    return Promise.reject(norm);
  }
);
```
