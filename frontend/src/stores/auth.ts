import { defineStore } from 'pinia'

function isJwt(tok?: string | null){ return !!tok && tok.split('.').length === 3 }

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('chs_token') as string | null }),
  actions: {
    setToken(t: string) { this.token = t; localStorage.setItem('chs_token', t) },
    clear() { this.token = null; localStorage.removeItem('chs_token') }
  },
  getters: { isAuthed: s => isJwt(s.token) }
})
