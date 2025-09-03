import axios from 'axios'
const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE })
api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('chs_token')
  if (token) cfg.headers = { ...(cfg.headers || {}), Authorization: `Bearer ${token}` }
  return cfg
})
api.interceptors.response.use(r => r, err => {
  // snackbars via custom event (UI store subscribed)
  window.dispatchEvent(new CustomEvent('chs:error', { detail: err }))
  return Promise.reject(err)
})
export default api
