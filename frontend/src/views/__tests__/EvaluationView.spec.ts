import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import EvaluationView from '@/views/EvaluationView.vue'

vi.mock('@/components/renderers/TableTile.vue', () => ({ default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub"><slot name="cta"/><slot name="item-actions"/></div>' } }))
vi.mock('@/components/renderers/ChartTile.vue', () => ({ default: { name: 'ChartTile', props: ['title','icon','vm','loading'], template: '<div class="chart-stub" />' } }))

vi.mock('@/services/evaluations', () => ({
  createEvaluation: async () => ({ evaluationId: 'e1' }),
  listEvaluations: async () => ([{ id: 'e1', baseline: 'm1', candidate: 'm2', status: 'PENDING', createdAt: new Date().toISOString() }]),
  getEvaluation: async () => ({ series: [{ name: 'topk', points: [{ x: 1, y: 0.7 }] }] }),
}))

describe('EvaluationView', () => {
  it('mounts and renders chart/table stubs', async () => {
    const wrapper = mount(EvaluationView, { global: { stubs: { 'v-btn': { template: '<button><slot/></button>' }, 'v-text-field': { template: '<input />' }, 'v-select': { template: '<select />' }, 'v-textarea': { template: '<textarea></textarea>' } } } })
    expect(wrapper.find('.table-stub').exists()).toBe(true)
    expect(wrapper.find('.chart-stub').exists()).toBe(true)
  })
})

