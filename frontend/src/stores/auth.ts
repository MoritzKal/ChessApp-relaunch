import { defineStore } from 'pinia'
import { getMe } from '@/services/users'

function isJwt(tok?: string | null){ return !!tok && tok.split('.').length === 3 }
function decodePayload(tok?: string | null): any {
  try {
    if (!isJwt(tok)) return null
    const p = tok!.split('.')[1]
    const json = decodeURIComponent(atob(p.replace(/-/g,'+').replace(/_/g,'/')).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
    return JSON.parse(json)
  } catch { return null }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('chs_token') as string | null, _validCheckedAt: 0, _roles: [] as string[] }),
  actions: {
    setToken(t: string) { this.token = t; localStorage.setItem('chs_token', t); this._roles = (decodePayload(t)?.roles||[]) as string[] },
    clear() { this.token = null; localStorage.removeItem('chs_token'); this._roles = []; this._validCheckedAt = 0 },
    async ensureValid(): Promise<boolean> {
      if (!this.isAuthed) return false
      const now = Date.now()
      if (now - this._validCheckedAt < 15000 && this._roles.length) return true
      try { const me = await getMe(); this._roles = (me?.roles||[]) as string[]; this._validCheckedAt = now; return true } catch { this.clear(); return false }
    }
  },
  getters: { isAuthed: s => isJwt(s.token), roles: (s) => (s._roles || []), isAdmin: (s) => (s._roles||[]).includes('ADMIN') }
})
