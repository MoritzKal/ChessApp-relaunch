import axios from 'axios'

// Kept for backward compatibility; prefer using lib/api.ts
const api = axios.create({ baseURL: (import.meta as any).env.VITE_API_BASE || '/api' })
api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('chs_token')
  if (token) {
    cfg.headers = cfg.headers ?? {}
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(cfg.headers as any).Authorization = `Bearer ${token}`
  }
  return cfg
})
api.interceptors.response.use(r => r, err => {
  window.dispatchEvent(new CustomEvent('chs:error', { detail: err }))
  return Promise.reject(err)
})
export default api
