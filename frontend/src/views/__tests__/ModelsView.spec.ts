import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ModelsView from '@/views/ModelsView.vue'

vi.mock('@/components/renderers/TableTile.vue', () => ({ default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub"><slot name="item-actions" v-for="row in (vm?.rows||[])" :item="row"/></div>' } }))
vi.mock('@/components/renderers/ChartTile.vue', () => ({ default: { name: 'ChartTile', props: ['title','icon','vm','loading'], template: '<div class="chart-stub" />' } }))

vi.mock('@/services/models', () => ({ listModels: async () => ([ { modelId: 'm1', displayName: 'Model 1', tags: ['prod'] } ]) }))
vi.mock('@/services/metrics', async () => { const mod = await vi.importActual<any>('@/services/metrics'); return { ...mod, getErrorRate: async () => ({ series: [] }) } })

describe('ModelsView', () => {
  beforeEach(() => { setActivePinia(createPinia()) })
  it('renders model rows and actions', async () => {
    const wrapper = mount(ModelsView, { global: { stubs: { 'v-btn': { template: '<button><slot/></button>' } } } })
    expect(wrapper.find('.table-stub').exists()).toBe(true)
    expect(wrapper.find('.chart-stub').exists()).toBe(true)
  })
})

