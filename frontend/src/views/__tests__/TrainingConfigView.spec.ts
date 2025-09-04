import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { h } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import TrainingConfigView from '@/views/TrainingConfigView.vue'

// Stubs for heavy tiles
vi.mock('@/components/panels/InfoMetricTile.vue', () => ({ default: { name: 'InfoMetricTile', template: '<div class="kpi-stub"></div>' } }))
vi.mock('@/components/renderers/ChartTile.vue', () => ({ default: { name: 'ChartTile', template: '<div class="chart-stub"></div>' } }))
// Custom stub for TableTile to expose cta slot and actions slot with a mock item
vi.mock('@/components/renderers/TableTile.vue', () => ({
  default: {
    name: 'TableTile',
    props: ['title','icon','vm','loading'],
    template: '<div class="table-stub"><slot name="cta"></slot><slot name="item-actions" :item="{ runId: \'run-1\', status: \'running\' }" /></div>'
  }
}))

// Mock services
const createTrainingMock = vi.fn().mockResolvedValue({ runId: 'run-123' })
const listTrainingRunsMock = vi.fn().mockResolvedValue([
  { runId: 'run-1', status: 'running', startedAt: new Date().toISOString(), finishedAt: null },
])
const controlTrainingRunMock = vi.fn().mockResolvedValue({ ok: true })

vi.mock('@/services/training', () => ({
  createTraining: (...args: any[]) => createTrainingMock(...args),
  listTrainingRuns: (...args: any[]) => listTrainingRunsMock(...args),
  controlTrainingRun: (...args: any[]) => controlTrainingRunMock(...args),
}))

vi.mock('@/services/datasets', () => ({
  listDatasets: vi.fn().mockResolvedValue([
    { id: 'ds1', name: 'Dataset One', version: 'v1', sizeRows: 100, locationUri: 's3://x', createdAt: new Date().toISOString() },
  ]),
  getDatasetVersions: vi.fn().mockResolvedValue([{ version: 'v1' }, { version: 'v2' }])
}))

vi.mock('@/services/models', () => ({
  listModels: vi.fn().mockResolvedValue([{ id: 'm1', name: 'Model One' }])
}))

describe('TrainingConfigView', () => {
  beforeEach(() => {
    createTrainingMock.mockClear()
    listTrainingRunsMock.mockClear()
    controlTrainingRunMock.mockClear()
    localStorage.removeItem('chs_training_form_v1')
    setActivePinia(createPinia())
  })

  const VFormStub = {
    name: 'v-form',
    inheritAttrs: false,
    setup(_p: any, { attrs, slots }: any) {
      return () => h('form', { ...(attrs as any) }, slots.default && slots.default())
    }
  }
  const stubs = {
    RouterLink: { template: '<a><slot/></a>' },
    'v-form': VFormStub,
    'v-btn': { template: '<button v-bind="$attrs"><slot/></button>' },
    'v-select': { template: '<select><slot/></select>' },
    'v-text-field': { template: '<input />' },
    'v-textarea': { template: '<textarea></textarea>' },
    'v-checkbox': { template: '<input type="checkbox" />' },
    'v-alert': { template: '<div class="alert"><slot/></div>' },
  }

  it('renders form and validates required fields', async () => {
    const wrapper = mount(TrainingConfigView, { global: { stubs } })
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 0))

    // click Start (expect validation to block)
    const startBtn = wrapper.find('button[type="submit"]')
    expect(startBtn.exists()).toBe(true)
    await startBtn.trigger('click')
    expect(createTrainingMock).not.toHaveBeenCalled()

    // Stay invalid -> no POST
  })

  it('shows actions in Active Runs and triggers control', async () => {
    const wrapper = mount(TrainingConfigView, { global: { stubs } })
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 0))

    const pauseBtn = wrapper.find('button[title="Pause"]')
    expect(pauseBtn.exists()).toBe(true)
    await pauseBtn.trigger('click')
    expect(controlTrainingRunMock).toHaveBeenCalledWith('run-1', 'pause')
  })
})
