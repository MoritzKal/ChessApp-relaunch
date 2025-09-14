import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { incApiCall, incApiError, reqStart, reqEnd } from '@/lib/obs'
import type { ApiError } from '@/types/common'
import router from '@/router'

const baseURL = (import.meta as any).env.VITE_API_BASE || '/api'
const devToken = (import.meta as any).env.VITE_DEV_STATIC_TOKEN
const autoDevToken = ((import.meta as any).env.VITE_AUTO_DEV_TOKEN || 'false') === 'true'
const TIMEOUT_MS = 10000
const MAX_RETRIES = 3

export const api = axios.create({ baseURL, timeout: TIMEOUT_MS, headers: { 'Content-Type': 'application/json' } })
const raw = axios.create({ baseURL, timeout: TIMEOUT_MS })

function isJwt(tok?: string | null) {
  return !!tok && tok.split('.').length === 3
}
function isAuthTokenPath(url?: string) {
  if (!url) return false
  return url.includes('/v1/auth/token') || url.includes('/auth/login')
}

// request: add Authorization and correlation id
api.interceptors.request.use((cfg) => {
  const lsToken = localStorage.getItem('chs_jwt') || localStorage.getItem('chs_token')
  const token = lsToken || (isJwt(devToken) ? devToken : undefined)
  cfg.headers = cfg.headers ?? {}
  // never attach auth for token endpoint or when explicitly skipped
  // @ts-expect-error custom flag is allowed
  const skipAuth = (cfg as any).__skipAuth === true || isAuthTokenPath(String(cfg.url))
  if (!skipAuth && token) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(cfg.headers as any).Authorization = `Bearer ${token}`
  }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ;(cfg.headers as any)['X-Correlation-Id'] = crypto.randomUUID()
  reqStart(); incApiCall();
  return cfg
})

// helper
function sleep(ms: number) { return new Promise((res) => setTimeout(res, ms)) }
function shouldRetry(error: AxiosError) {
  const cfg = error.config
  const method = (cfg?.method || 'get').toLowerCase()
  if (method !== 'get') return false
  const status = error.response?.status
  if (!status) return true // network or timeout
  return status >= 500 && status < 600
}
function backoff(attempt: number) { // 0..MAX_RETRIES-1
  const base = 250
  return base * Math.pow(2, attempt) // 250, 500, 1000
}

function normalizeError(err: AxiosError): ApiError {
  const status = err.response?.status ?? 0
  const data: any = err.response?.data || {}
  const message = data?.message || err.message || 'Request failed'
  let code: ApiError['code'] = 'INTERNAL'
  if (status === 401) code = 'UNAUTHORIZED'
  else if (status === 404) code = 'NOT_FOUND'
  else if (status === 429) code = 'RATE_LIMIT'
  else if (status >= 400 && status < 500) code = 'VALIDATION'
  return { ok: false, status, code, message, details: data?.errors || data?.details }
}

// response: retry on eligible errors, then normalize
let tokenPromise: Promise<string | null> | null = null
let lastTokenAttempt = 0
async function ensureDevToken() {
  if (!autoDevToken) return null
  const now = Date.now()
  if (now - lastTokenAttempt < 30000 && tokenPromise) return tokenPromise
  lastTokenAttempt = now
  tokenPromise = (async () => {
    try {
      const res = await raw.get('/v1/auth/token', {
        // do NOT send Authorization; mark to skip just in case
        // @ts-expect-error internal flag
        __skipAuth: true,
        params: { user: 'dev', roles: 'USER', scope: 'api.read', ttl: 3600 }
      } as any)
      const token = (res.data as any)?.token
      if (token && isJwt(token)) { localStorage.setItem('chs_token', token); return token }
    } catch { /* ignore */ }
    return null
  })()
  const t = await tokenPromise
  tokenPromise = null
  return t
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

api.interceptors.response.use(r => { reqEnd(); return r }, async (error: AxiosError) => {
  reqEnd()
  const cfg = (error.config || {}) as AxiosRequestConfig & { __retryCount?: number }
  // 401 handler: try to fetch a dev token once and retry (not for token endpoint)
  if (error.response?.status === 401 && !(cfg as any).__retriedAuth && !isAuthTokenPath(String(cfg.url))) {
    (cfg as any).__retriedAuth = true
    const t = await ensureDevToken()
    if (t) {
      cfg.headers = cfg.headers ?? {}
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(cfg.headers as any).Authorization = `Bearer ${t}`
      return api.request(cfg)
    }
  }
  cfg.__retryCount = cfg.__retryCount ?? 0
  if (cfg.__retryCount < MAX_RETRIES && shouldRetry(error)) {
    const delay = backoff(cfg.__retryCount)
    cfg.__retryCount++
    await sleep(delay)
    return api.request(cfg)
  }
  const norm = normalizeError(error)
  incApiError(norm)
  // broadcast for global snackbars if any consumer wants it
  window.dispatchEvent(new CustomEvent('chs:error', { detail: norm }))
  return Promise.reject(norm)
})

export default api

