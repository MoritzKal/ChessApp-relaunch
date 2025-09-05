import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ObservabilityView from '@/views/ObservabilityView.vue'

vi.mock('@/components/renderers/ChartTile.vue', () => ({ default: { name: 'ChartTile', props: ['title','icon','vm','loading'], template: '<div class="chart-stub" />' } }))
vi.mock('@/components/renderers/TableTile.vue', () => ({ default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub" />' } }))

vi.mock('@/plugins/obsAxios', () => ({ default: { get: vi.fn().mockResolvedValue({ data: { status: 'success', data: { result: [ { values: [] } ] } } }) } }))
vi.mock('@/services/metrics', async () => { const mod = await vi.importActual<any>('@/services/metrics'); return { ...mod, getRps: async () => ({ series: [] }), getErrorRate: async () => ({ series: [] }), getLoss: async () => ({ series: [] }) } })

describe('ObservabilityView', () => {
  beforeEach(() => { setActivePinia(createPinia()) })
  it('renders chart and table tiles', async () => {
    const wrapper = mount(ObservabilityView)
    expect(wrapper.findAll('.chart-stub').length).toBeGreaterThan(0)
    expect(wrapper.find('.table-stub').exists()).toBe(true)
  })
})

