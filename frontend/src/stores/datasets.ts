import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ApiError } from '@/types/common'
import type { Dataset, DatasetSummary } from '@/types/datasets'
import { countDatasets, getDataset, getDatasetSummary, listDatasets, getDatasetVersions, getChesscomArchives, importChesscom as svcImportChesscom, getIngestRun } from '@/services/datasets'
import type { TableVM } from '@/types/vm'

export interface PollTarget { key: string; intervalMs: number; run: () => Promise<void> }

export const useDatasetsStore = defineStore('datasets', () => {
  const byId = ref(new Map<string, Dataset>())
  const summaryById = ref(new Map<string, DatasetSummary>())
  const versionsById = ref(new Map<string, any[]>())
  const listCache = ref(new Map<string, Dataset[]>()) // key: `limit:offset:sort`
  const counts = ref<number | null>(null)

  const loading = ref(new Set<string>())
  const errors = ref(new Map<string, ApiError>())

  const keyList = (limit?: number, offset?: number, sort?: string) => `l:${limit ?? 50}|o:${offset ?? 0}|s:${sort ?? ''}`

  function setLoading(k: string, v: boolean) { v ? loading.value.add(k) : loading.value.delete(k) }
  function setError(k: string, e?: ApiError | null) { if (e) errors.value.set(k, e); else errors.value.delete(k) }

  async function fetchCount() {
    const k = 'count'
    try { setLoading(k, true); setError(k, null); counts.value = await countDatasets() }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchDataset(id: string) {
    const k = `get:${id}`
    try { setLoading(k, true); setError(k, null); byId.value.set(id, await getDataset(id)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchSummary(id: string) {
    const k = `summary:${id}`
    try { setLoading(k, true); setError(k, null); summaryById.value.set(id, await getDatasetSummary(id)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchVersions(id: string) {
    const k = `versions:${id}`
    try { setLoading(k, true); setError(k, null); versionsById.value.set(id, await getDatasetVersions(id)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchList(limit?: number, offset?: number, sort?: string) {
    const key = keyList(limit, offset, sort)
    try { setLoading(key, true); setError(key, null); listCache.value.set(key, await listDatasets({ limit, offset, sort })) }
    catch (e: any) { setError(key, e) }
    finally { setLoading(key, false) }
  }

  async function fetchTop(sort: 'size' | 'rows', limit = 20) {
    return fetchList(limit, 0, sort)
  }

  async function batchFetch(ids: string[], opts?: { summary?: boolean; versions?: boolean }) {
    await Promise.all(ids.map(async (id) => {
      await fetchDataset(id)
      if (opts?.summary) await fetchSummary(id)
      if (opts?.versions) await fetchVersions(id)
    }))
  }

  // Chess.com archives cache per user
  const chesscomMonths = ref(new Map<string, string[]>())
  async function fetchChesscomArchives(user: string) {
    const norm = (user || '').trim().toLowerCase()
    const k = `chesscom.archives:${norm}`
    try {
      setLoading(k, true); setError(k, null)
      const resp = await getChesscomArchives(norm)
      chesscomMonths.value.set(norm, resp.months || [])
    } catch (e: any) {
      setError(k, e)
      throw e
    } finally { setLoading(k, false) }
  }

  async function importChesscom(p: { user: string; months: string[]; datasetId?: string; note?: string }) {
    const body = { user: (p.user||'').trim().toLowerCase(), months: p.months, datasetId: p.datasetId, note: p.note }
    const k = `ingest.chesscom:${body.user}`
    try {
      setLoading(k, true); setError(k, null)
      return await svcImportChesscom(body)
    } catch (e: any) { setError(k, e); throw e } finally { setLoading(k, false) }
  }

  async function pollIngest(runId: string, opts?: { intervalMs?: number }) {
    const iv = opts?.intervalMs ?? 1000
    // simple polling loop that resolves when run not running
    // caller handles success redirect
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const r = await getIngestRun(runId)
      if (r.status !== 'running') return r
      await new Promise((res) => setTimeout(res, iv))
    }
  }

  // selectors (memoized via computed closures)
  function selectDataset(id: string) { return computed(() => byId.value.get(id) || null) }
  function selectSummary(id: string) { return computed(() => summaryById.value.get(id) || null) }
  function selectVersions(id: string) { return computed(() => versionsById.value.get(id) || []) }
  function selectList(limit?: number, offset?: number, sort?: string) {
    const key = keyList(limit, offset, sort)
    return computed(() => listCache.value.get(key) || [])
  }
  function selectTop(sort: 'size' | 'rows', limit = 20) { return selectList(limit, 0, sort) }

  function selectTopTableVm(sort: 'size' | 'rows', limit = 20) {
    const list = selectTop(sort, limit)
    return computed<TableVM>(() => ({
      columns: [
        { key: 'name', label: 'Name' },
        { key: 'version', label: 'Version' },
        { key: 'rows', label: 'Rows', align: 'end' },
        { key: 'createdAt', label: 'Created' },
      ],
      rows: list.value.map(d => ({ name: d.name, version: d.version, rows: d.sizeRows, createdAt: d.createdAt }))
    }))
  }

  // declare poll targets (no timers here)
  const pollTargets = computed<PollTarget[]>(() => ([
    { key: 'datasets.count', intervalMs: 5000, run: fetchCount },
  ]))

  return {
    // state
    byId, summaryById, versionsById, listCache, counts, loading, errors,
    // actions
    fetchCount, fetchDataset, fetchSummary, fetchVersions, fetchList, fetchTop, batchFetch,
    fetchChesscomArchives, importChesscom, pollIngest,
    // selectors
    selectDataset, selectSummary, selectVersions, selectList, selectTop, selectTopTableVm,
    // polling declaration
    pollTargets,
    chesscomMonths,
  }
})
