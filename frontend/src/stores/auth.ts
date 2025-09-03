import { defineStore } from 'pinia'
export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('chs_token') as string | null }),
  actions: {
    setToken(t: string) { this.token = t; localStorage.setItem('chs_token', t) },
    clear() { this.token = null; localStorage.removeItem('chs_token') }
  },
  getters: { isAuthed: s => !!s.token }
})
