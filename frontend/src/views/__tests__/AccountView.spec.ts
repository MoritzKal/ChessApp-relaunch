import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AccountView from '@/views/AccountView.vue'

vi.mock('@/components/renderers/TableTile.vue', () => ({ default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub"><slot name="tools"/><slot name="cta"/></div>' } }))

vi.mock('@/services/users', () => ({
  getMe: async () => ({ username: 'u1', createdAt: new Date().toISOString(), roles: ['USER'] }),
  listTokens: async () => ([{ id: 't1', createdAt: new Date().toISOString(), lastUsedAt: null }]),
  createToken: async () => ({ id: 't2' }),
  revokeToken: async () => {},
  getPrefs: async () => ({ theme: 'dark', locale: 'de' }),
  putPrefs: async () => {},
}))

vi.mock('@/services/games', async () => {
  const mod = await vi.importActual<any>('@/services/games')
  return { ...mod, recentGames: async () => ([])}
})

describe('AccountView', () => {
  beforeEach(() => { setActivePinia(createPinia()) })
  it('mounts and renders tables', async () => {
    const wrapper = mount(AccountView, { global: { stubs: { 'v-btn': { template: '<button><slot/></button>' }, 'v-text-field': { template: '<input />' }, 'v-switch': { template: '<input type="checkbox" />' }, 'v-select': { template: '<select />' }, 'v-textarea': { template: '<textarea></textarea>' } } } })
    expect(wrapper.findAll('.table-stub').length).toBeGreaterThan(0)
  })
})

