import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ApiError } from '@/types/common'
import type { ScalarMetric, TimeseriesResponse, HealthAggregate } from '@/types/metrics'
import { getHealth, getLoss, getRps, getErrorRate, getLatency, getMps, getElo, getThroughput, getTrainingMetric } from '@/services/metrics'

export interface PollTarget { key: string; intervalMs: number; run: () => Promise<void> }

export const useMetricsStore = defineStore('metrics', () => {
  const scalars = ref(new Map<string, ScalarMetric>())
  const series = ref(new Map<string, TimeseriesResponse>())
  const health = ref<HealthAggregate | null>(null)
  const loading = ref(new Set<string>())
  const errors = ref(new Map<string, ApiError>())

  function setLoading(k: string, v: boolean) { v ? loading.value.add(k) : loading.value.delete(k) }
  function setError(k: string, e?: ApiError | null) { if (e) errors.value.set(k, e); else errors.value.delete(k) }

  async function fetchHealth() {
    const k = 'health'
    try { setLoading(k, true); setError(k, null); health.value = await getHealth() }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchScalar(key: string, run: () => Promise<ScalarMetric>) {
    try { setLoading(key, true); setError(key, null); scalars.value.set(key, await run()) }
    catch (e: any) { setError(key, e) }
    finally { setLoading(key, false) }
  }

  async function fetchSeries(key: string, run: () => Promise<TimeseriesResponse>) {
    try { setLoading(key, true); setError(key, null); series.value.set(key, await run()) }
    catch (e: any) { setError(key, e) }
    finally { setLoading(key, false) }
  }

  // convenience wrappers
  const fetchLoss = (range: string) => fetchSeries(`loss:${range}`, () => getLoss(range))
  const fetchRps = (range: string) => fetchSeries(`rps:${range}`, () => getRps(range))
  const fetchErrorRate = (range: string) => fetchSeries(`error_rate:${range}`, () => getErrorRate(range))
  const fetchLatencyP50 = () => fetchScalar('latency:p50', () => getLatency(50))
  const fetchLatencyP95 = () => fetchScalar('latency:p95', () => getLatency(95))
  const fetchMps = () => fetchScalar('mps', () => getMps())
  const fetchElo = (range: string) => fetchSeries(`elo:${range}`, () => getElo(range))
  const fetchThroughput = (runId: string) => fetchScalar(`throughput:${runId}`, () => getThroughput(runId))
  const fetchTrainingSeries = (runId: string, m: string, range?: string) => fetchSeries(`train:${runId}:${m}:${range ?? ''}`, () => getTrainingMetric(runId, m, range))

  // selectors
  function selectScalar(key: string) { return computed(() => scalars.value.get(key) || null) }
  function selectSeries(key: string) { return computed(() => series.value.get(key) || null) }
  const selectHealth = computed(() => health.value)

  // declared poll targets (no timers)
  const pollTargets = computed<PollTarget[]>(() => ([
    { key: 'metrics.health', intervalMs: 5000, run: fetchHealth },
    { key: 'metrics.rps.7d', intervalMs: 5000, run: () => fetchRps('7d') },
  ]))

  return {
    scalars, series, health, loading, errors,
    fetchHealth, fetchLoss, fetchRps, fetchErrorRate, fetchLatencyP50, fetchLatencyP95, fetchMps, fetchElo, fetchThroughput, fetchTrainingSeries,
    selectScalar, selectSeries, selectHealth,
    pollTargets,
  }
})

