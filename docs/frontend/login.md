# Login (Auth)
**Route:** /login  
**Felder:** username, password, rememberMe  
**Prod-Flow:** POST /v1/auth/login → {accessToken, ...}; Token via Axios-Interceptor (Authorization: Bearer).  
**Dev-Fallback:** VITE_DEV_STATIC_TOKEN (Login akzeptiert Dummy-Creds).  
**Guards:** Alle Routen außer /login geschützt.  
**Fehler:** 401/403 → Snackbar + "View Logs".
