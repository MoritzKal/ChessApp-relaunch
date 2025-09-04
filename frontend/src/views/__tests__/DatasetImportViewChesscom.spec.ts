import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import DatasetImportView from '@/views/DatasetImportView.vue'

// Stubs
vi.mock('@/components/renderers/TableTile.vue', () => ({
  default: { name: 'TableTile', props: ['title','icon','vm','loading'], template: '<div class="table-stub"><slot name="cta"></slot></div>' }
}))

const getChesscomArchivesMock = vi.fn()
const importChesscomMock = vi.fn()
const getIngestRunMock = vi.fn()

vi.mock('@/services/datasets', async () => {
  const mod = await vi.importActual<any>('@/services/datasets')
  return {
    ...mod,
    getChesscomArchives: (...args:any[]) => getChesscomArchivesMock(...args),
    importChesscom: (...args:any[]) => importChesscomMock(...args),
    getIngestRun: (...args:any[]) => getIngestRunMock(...args),
  }
})

describe('DatasetImportView (Chess.com)', () => {
  beforeEach(() => { setActivePinia(createPinia()); getChesscomArchivesMock.mockReset(); importChesscomMock.mockReset(); getIngestRunMock.mockReset() })

  const stubs = {
    RouterLink: { template: '<a><slot/></a>' },
    'v-form': { name: 'v-form', template: '<form><slot/></form>' },
    'v-btn': { template: '<button v-bind="$attrs"><slot/></button>' },
    'v-text-field': { template: '<input v-bind="$attrs" />' },
    'v-textarea': { template: '<textarea></textarea>' },
    'v-file-input': { template: '<input type="file" />' },
    'v-alert': { template: '<div class="alert"><slot/></div>' },
    'v-tabs': { template: '<div class="tabs"><slot/></div>' },
    'v-tab': { template: '<button class="tab"><slot/></button>' },
    'v-window': { template: '<div class="win"><slot/></div>' },
    'v-window-item': { template: '<div class="win-item"><slot/></div>' },
    'v-checkbox': { template: '<input type="checkbox" />' },
  }

  it('loads archives for user via button', async () => {
    getChesscomArchivesMock.mockResolvedValueOnce({ months: [ '2025-02', '2025-01', '2024-12' ] })
    const wrapper = mount(DatasetImportView, { global: { stubs } })
    ;(wrapper.vm as any).tab = 'chesscom'
    ;(wrapper.vm as any).username = 'MyUser'
    await nextTick()

    // trigger load explicitly
    await (wrapper.vm as any).loadArchives()
    expect(getChesscomArchivesMock).toHaveBeenCalledWith('myuser')
    expect((wrapper.vm as any).months.length).toBe(3)
  })

  it('selects all and then range subset; imports with correct body and polls to success', async () => {
    getChesscomArchivesMock.mockResolvedValueOnce({ months: [ '2025-02', '2025-01', '2024-12', '2024-11' ] })
    importChesscomMock.mockResolvedValueOnce({ runId: 'ing-xyz', status: 'queued' })
    // two polls: running -> success
    getIngestRunMock.mockResolvedValueOnce({ runId: 'ing-xyz', status: 'running' })
    getIngestRunMock.mockResolvedValueOnce({ runId: 'ing-xyz', status: 'success', datasetId: 'chesscom_myuser' })

    const wrapper = mount(DatasetImportView, { global: { stubs } })
    ;(wrapper.vm as any).tab = 'chesscom'
    ;(wrapper.vm as any).username = 'MyUser'
    await (wrapper.vm as any).loadArchives()

    // select all
    await (wrapper.vm as any).selectAll()
    expect((wrapper.vm as any).selected.size).toBe(4)

    // select range
    ;(wrapper.vm as any).fromMonth = '2024-12'
    ;(wrapper.vm as any).toMonth = '2025-01'
    await (wrapper.vm as any).selectRange()
    expect(Array.from((wrapper.vm as any).selected)).toEqual(['2025-01','2024-12'])

    // start import
    await (wrapper.vm as any).startChessImport()
    expect(importChesscomMock).toHaveBeenCalled()
    const body = importChesscomMock.mock.calls[0][0]
    expect(body.user).toBe('myuser')
    expect(body.months).toEqual(['2025-01','2024-12'])
    expect(body.datasetId).toBe('chesscom_myuser')

    // wait poll
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.html()).toContain('Zum Datensatz')
  })
})
