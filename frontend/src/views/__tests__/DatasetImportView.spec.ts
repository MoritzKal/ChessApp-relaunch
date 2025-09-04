import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { h, nextTick } from 'vue'
import DatasetImportView from '@/views/DatasetImportView.vue'

// Stubs
vi.mock('@/components/renderers/TableTile.vue', () => ({
  default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub"><slot name="cta"></slot></div>' }
}))

const startIngestMock = vi.fn().mockResolvedValue({ runId: 'ing-1', datasetId: 'dsX' })
const getIngestRunMock = vi.fn()

vi.mock('@/services/datasets', async () => {
  const mod = await vi.importActual<any>('@/services/datasets')
  return { ...mod, startIngest: (...args:any[]) => startIngestMock(...args), getIngestRun: (...args:any[]) => getIngestRunMock(...args) }
})

describe('DatasetImportView', () => {
  beforeEach(() => { startIngestMock.mockClear(); getIngestRunMock.mockReset() })

  const stubs = {
    RouterLink: { template: '<a><slot/></a>' },
    'v-form': { name: 'v-form', setup(_p:any,{ slots }: any){ return () => h('form', { onSubmit: (e:any)=>e.preventDefault() }, slots.default && slots.default()) } },
    'v-btn': { template: '<button v-bind="$attrs"><slot/></button>' },
    'v-text-field': { template: '<input v-bind="$attrs" />' },
    'v-textarea': { template: '<textarea></textarea>' },
    'v-file-input': { template: '<input type="file" />' },
    'v-alert': { template: '<div class="alert"><slot/></div>' },
  }

  it('uploads and polls to success', async () => {
    // immediate success on first poll (start() does immediate tick)
    getIngestRunMock.mockResolvedValueOnce({ runId: 'ing-1', status: 'success', datasetId: 'dsX' })

    const wrapper = mount(DatasetImportView, { global: { stubs } })
    await nextTick()

    // set fields via component instance proxies
    ;(wrapper.vm as any).datasetName = 'dsX'
    ;(wrapper.vm as any).file = new File(["pgn"], 'f.pgn', { type: 'text/plain' })

    const form = wrapper.find('form')
    await form.trigger('submit')

    expect(startIngestMock).toHaveBeenCalled()
    // wait a tick for poller to update state
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.html()).toContain('Zum Datensatz')
  })
})
