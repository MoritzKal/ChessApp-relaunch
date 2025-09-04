import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ApiError } from '@/types/common'
import type { TrainingRun } from '@/types/training'
import { getTraining, listTrainingRuns, countTraining } from '@/services/training'

export interface PollTarget { key: string; intervalMs: number; run: () => Promise<void> }

export const useTrainingStore = defineStore('training', () => {
  const byRunId = ref(new Map<string, TrainingRun>())
  const runsList = ref(new Map<string, TrainingRun[]>()) // key: `limit:offset`
  const countsByStatus = ref(new Map<string, number>())

  const loading = ref(new Set<string>())
  const errors = ref(new Map<string, ApiError>())

  function setLoading(k: string, v: boolean) { v ? loading.value.add(k) : loading.value.delete(k) }
  function setError(k: string, e?: ApiError | null) { if (e) errors.value.set(k, e); else errors.value.delete(k) }
  const keyList = (limit?: number, offset?: number) => `l:${limit ?? 50}|o:${offset ?? 0}`

  async function fetchRun(runId: string) {
    const k = `get:${runId}`
    try { setLoading(k, true); setError(k, null); byRunId.value.set(runId, await getTraining(runId)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchRuns(limit?: number, offset?: number) {
    const key = keyList(limit, offset)
    try { setLoading(key, true); setError(key, null); runsList.value.set(key, await listTrainingRuns({ limit, offset })) }
    catch (e: any) { setError(key, e) }
    finally { setLoading(key, false) }
  }

  async function fetchCount(status?: string) {
    const k = `count:${status ?? 'all'}`
    try { setLoading(k, true); setError(k, null); countsByStatus.value.set(status ?? 'all', await countTraining({ status })) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  function selectRun(runId: string) { return computed(() => byRunId.value.get(runId) || null) }
  function selectRuns(limit?: number, offset?: number) {
    const key = keyList(limit, offset)
    return computed(() => runsList.value.get(key) || [])
  }

  const pollTargets = computed<PollTarget[]>(() => ([
    { key: 'training.count.active', intervalMs: 5000, run: () => fetchCount('active') },
    { key: 'training.count.total', intervalMs: 10000, run: () => fetchCount('total') },
  ]))

  return {
    byRunId, runsList, countsByStatus, loading, errors,
    fetchRun, fetchRuns, fetchCount,
    selectRun, selectRuns,
    pollTargets,
  }
})
