import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { h, nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import DatasetsIndexView from '@/views/DatasetsIndexView.vue'

// Tile stubs
vi.mock('@/components/renderers/TableTile.vue', () => ({
  default: {
    name: 'TableTile',
    props: ['title','icon','vm','loading'],
    template: '<div class="table-stub"><slot name="tools"></slot><slot name="cta"></slot><slot name="item-actions" v-for="row in (vm?.rows||[])" :item="row" /></div>'
  }
}))

// Mock dataset service
const listDatasetsMock = vi.fn().mockResolvedValue([
  { id: 'ds1', name: 'A', version: 'v1', sizeRows: 10, sizeBytes: 1024, createdAt: new Date().toISOString(), versions: { count: 1 } },
  { id: 'ds2', name: 'B', version: 'v2', sizeRows: 20, sizeBytes: 2048, createdAt: new Date().toISOString(), versions: { count: 2 } },
])
vi.mock('@/services/datasets', async () => {
  const mod = await vi.importActual<any>('@/services/datasets')
  return { ...mod, listDatasets: (...args: any[]) => listDatasetsMock(...args), datasetExportUrl: (id: string, q:any) => `/api/v1/datasets/${id}/export?format=${q.format}&version=${q.version||'latest'}` }
})

describe('DatasetsIndexView', () => {
  beforeEach(() => { setActivePinia(createPinia()); listDatasetsMock.mockClear() })

  const stubs = {
    RouterLink: { template: '<a><slot/></a>' },
    'v-btn': { template: '<button v-bind="$attrs"><slot/></button>' },
    'v-select': { template: '<select v-bind="$attrs"><slot/></select>' },
    'v-text-field': { template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />', props: ['modelValue'] },
    'v-spacer': { template: '<span />' }
  }

  it('renders and triggers search', async () => {
    const wrapper = mount(DatasetsIndexView, { global: { stubs } })
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect(listDatasetsMock).toHaveBeenCalled()

    // simulate pressing enter in search
    const inputs = wrapper.findAll('input')
    const search = inputs.at(0)
    await search?.setValue('abc')
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect(listDatasetsMock).toHaveBeenCalledWith({ limit: 20, offset: 0, sort: undefined, q: 'abc' })
  })

  it('renders export action with correct href', async () => {
    const wrapper = mount(DatasetsIndexView, { global: { stubs } })
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    const btns = wrapper.findAll('button[title="Export"]')
    // the anchor wraps the button; assert existence via anchor
    const anchors = wrapper.findAll('a[href*="/v1/datasets/"]')
    expect(anchors.length).toBeGreaterThan(0)
    expect(anchors[0].attributes('href')).toContain('/v1/datasets/ds1/export?format=pgn')
  })
})
