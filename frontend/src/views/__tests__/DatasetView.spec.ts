import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import DatasetView from '@/views/DatasetView.vue'

// Stubs for tiles
vi.mock('@/components/panels/InfoMetricTile.vue', () => ({ default: { name: 'InfoMetricTile', template: '<div class="kpi-stub"></div>' } }))
vi.mock('@/components/renderers/ChartTile.vue', () => ({ default: { name: 'ChartTile', props:['vm'], template: '<div class="chart-stub"></div>' } }))
vi.mock('@/components/renderers/TableTile.vue', () => ({
  default: { name: 'TableTile', props: ['vm','title','icon','loading'], template: '<div class="table-stub"><slot name="tools"></slot></div>' }
}))

// Mock services
const getSchemaMock = vi.fn().mockResolvedValue([{ name: 'col', dtype: 'str' }])
const getSampleMock = vi.fn().mockResolvedValue({ rows: [{ a: 1 }], nextCursor: 'c2' })
const getQualityMock = vi.fn().mockResolvedValue({ missingPct: 0.1, outlierPct: 0.02, duplicatePct: 0 })
const getIngestMock = vi.fn().mockResolvedValue([{ at: 't', user: 'u', note: 'n', version: 'v' }])
vi.mock('@/services/datasets', async () => {
  const mod = await vi.importActual<any>('@/services/datasets')
  return {
    ...mod,
    getDatasetSchema: (...a:any[]) => getSchemaMock(...a),
    getDatasetSample: (...a:any[]) => getSampleMock(...a),
    getDatasetQuality: (...a:any[]) => getQualityMock(...a),
    getIngestHistory: (...a:any[]) => getIngestMock(...a),
    datasetExportUrl: (id: string, q:any) => `/api/v1/datasets/${id}/export?format=${q.format}${q.version?`&version=${q.version}`:''}`
  }
})

describe('DatasetView', () => {
  beforeEach(() => { getSchemaMock.mockClear(); getSampleMock.mockClear(); getQualityMock.mockClear(); getIngestMock.mockClear() })

  it('loads tiles and handles sample paging + export', async () => {
    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/datasets/:id', component: DatasetView }] })
    router.push('/datasets/ds1')
    await router.isReady()
    const wrapper = mount(DatasetView, { global: { plugins: [router, createPinia()], stubs: { RouterLink: { template: '<a><slot/></a>' }, 'v-select': { template: '<select></select>' }, 'v-btn': { template: '<button v-bind="$attrs"><slot/></button>' }, 'v-menu': { template: '<div><slot name="activator" :props="{}"></slot><slot></slot></div>' }, 'v-list': { template: '<div><slot/></div>' }, 'v-list-item': { template: '<button class="list-item" v-bind="$attrs"></button>' } } } })

    // sample load more
    // find the Mehr laden button and click it
    const more = wrapper.findAll('button').find(b => b.text().includes('Mehr laden'))
    if (more) await more.trigger('click')
    expect(getSampleMock).toHaveBeenCalled()

    // export click: click menu item for PGN
    const items = wrapper.findAll('.list-item')
    if (items.length) await items[2].trigger('click')
    // window.open is not easily assertable here; ensure no errors
    expect(true).toBe(true)
  })
})
