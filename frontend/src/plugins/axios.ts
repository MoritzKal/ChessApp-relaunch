import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiError } from '@/types/common'

const baseURL = (import.meta as any).env.VITE_API_BASE || '/api'
const devToken = (import.meta as any).env.VITE_DEV_STATIC_TOKEN
const TIMEOUT_MS = 10000
const MAX_RETRIES = 3

export const api = axios.create({ baseURL, timeout: TIMEOUT_MS, headers: { 'Content-Type': 'application/json' } })

// request: add Authorization and correlation id
api.interceptors.request.use((cfg) => {
  const lsToken = localStorage.getItem('chs_jwt') || localStorage.getItem('chs_token')
  const token = lsToken || devToken
  cfg.headers = cfg.headers ?? {}
  if (token) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(cfg.headers as any).Authorization = `Bearer ${token}`
  }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ;(cfg.headers as any)['X-Correlation-Id'] = crypto.randomUUID()
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
api.interceptors.response.use(r => r, async (error: AxiosError) => {
  const cfg = (error.config || {}) as AxiosRequestConfig & { __retryCount?: number }
  cfg.__retryCount = cfg.__retryCount ?? 0
  if (cfg.__retryCount < MAX_RETRIES && shouldRetry(error)) {
    const delay = backoff(cfg.__retryCount)
    cfg.__retryCount++
    await sleep(delay)
    return api.request(cfg)
  }
  const norm = normalizeError(error)
  // broadcast for global snackbars if any consumer wants it
  window.dispatchEvent(new CustomEvent('chs:error', { detail: norm }))
  return Promise.reject(norm)
})

export default api
