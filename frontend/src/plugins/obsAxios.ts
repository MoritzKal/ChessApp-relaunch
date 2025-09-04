import axios from 'axios'

const baseURL = (import.meta as any).env.VITE_OBS_BASE || ''
const apiKey = (import.meta as any).env.VITE_OBS_API_KEY || ''
const TIMEOUT_MS = 8000

export const obsApi = axios.create({ baseURL, timeout: TIMEOUT_MS })

obsApi.interceptors.request.use((cfg) => {
  cfg.headers = cfg.headers ?? {}
  if (apiKey) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(cfg.headers as any)['X-Obs-Api-Key'] = apiKey
  }
  return cfg
})

export default obsApi

