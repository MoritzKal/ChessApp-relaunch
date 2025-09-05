import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PlayGameView from '@/views/PlayGameView.vue'

// Stubs
vi.mock('@/components/renderers/ChartTile.vue', () => ({ default: { name: 'ChartTile', props: ['title','icon','vm','loading'], template: '<div class="chart-stub" />' } }))
vi.mock('@/components/renderers/TableTile.vue', () => ({ default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub"><slot name="tools"/></div>' } }))

// Mock metrics services
vi.mock('@/services/metrics', async () => {
  const mod = await vi.importActual<any>('@/services/metrics')
  return {
    ...mod,
    getLoss: async () => ({ series: [] }),
    getRps: async () => ({ series: [] }),
    getErrorRate: async () => ({ series: [] }),
    getLatency: async () => ({ value: 12 }),
    getMps: async () => ({ value: 3 }),
  }
})

// Mock obs api
vi.mock('@/plugins/obsAxios', () => ({ default: { get: vi.fn().mockResolvedValue({ data: { status: 'success', data: { result: [ { values: [] } ] } } }) } }))

describe('PlayGameView', () => {
  beforeEach(() => { setActivePinia(createPinia()) })
  it('mounts and shows stubs', async () => {
    const wrapper = mount(PlayGameView, { global: { stubs: { 'v-btn': { template: '<button><slot/></button>' } } } })
    expect(wrapper.find('.chart-stub').exists()).toBe(true)
    expect(wrapper.find('.table-stub').exists()).toBe(true)
  })
})

