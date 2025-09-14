import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ApiError } from '@/types/common'
import type { ScalarMetric, TimeseriesResponse, HealthAggregate } from '@/types/metrics'
import { getHealth, getLoss, getRps, getErrorRate, getLatency, getMps, getElo, getThroughput, getTrainingMetric, getUtilization } from '@/services/metrics'
import type { SeriesVM } from '@/types/vm'

export interface PollTarget { key: string; intervalMs: number; run: () => Promise<void> }

export const useMetricsStore = defineStore('metrics', () => {
  const scalars = ref(new Map<string, ScalarMetric>())
  const series = ref(new Map<string, TimeseriesResponse>())
  const seriesOrder = ref<string[]>([]) // LRU for eviction
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

  function evictIfNeeded() {
    while (seriesOrder.value.length > MAX_SERIES_KEYS) {
      const oldKey = seriesOrder.value.shift()!
      series.value.delete(oldKey)
      errors.value.delete(oldKey)
      loading.value.delete(oldKey)
    }
  }

  async function fetchSeries(key: string, run: () => Promise<TimeseriesResponse>) {
    try {
      setLoading(key, true); setError(key, null)
      const resp = await run()
      series.value.set(key, prune(resp))
      // maintain LRU order and evict if needed
      const idx = seriesOrder.value.indexOf(key)
      if (idx !== -1) seriesOrder.value.splice(idx, 1)
      seriesOrder.value.push(key)
      evictIfNeeded()
    }
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
  const fetchThroughput = (runId?: string, range = '24h') => fetchSeries(`throughput:${runId || 'all'}:${range}`, () => getThroughput(runId, range))
  const fetchTrainingSeries = (runId: string, m: string, range?: string) => fetchSeries(`train:${runId}:${m}:${range ?? ''}`, () => getTrainingMetric(runId, m, range))
  const fetchUtilization = (runId: string, range = '24h') => fetchSeries(`util:${runId}:${range}`, () => getUtilization(runId, range))

  // selectors
  function selectScalar(key: string) { return computed(() => scalars.value.get(key) || null) }
  function selectSeries(key: string) { return computed(() => series.value.get(key) || null) }
  const selectHealth = computed(() => health.value)

  // VM mapper for charts
  function selectSeriesVm(key: string, label?: string, targetPoints = 1200) {
    const thin = (arr: { ts: string; value: number }[]) => {
      if (!arr || arr.length <= targetPoints) return arr
      const step = Math.ceil(arr.length / targetPoints)
      const res: { ts: string; value: number }[] = []
      for (let i = 0; i < arr.length; i += step) res.push(arr[i])
      return res
    }
    return computed<SeriesVM | null>(() => {
      const s = series.value.get(key)
      if (!s) return null
      return {
        range: s.range,
        series: s.series.map((ss) => {
          const pts = thin(ss.points)
          return {
            label: label || ss.metric,
            data: pts.map(p => ({ x: new Date(p.ts).getTime(), y: p.value }))
          }
        })
      }
    })
  }

  // declared poll targets (no timers)
  const pollTargets = computed<PollTarget[]>(() => ([
    { key: 'metrics.health', intervalMs: 5000, run: fetchHealth },
    { key: 'metrics.rps.7d', intervalMs: 5000, run: () => fetchRps('7d') },
  ]))

  return {
    scalars, series, health, loading, errors,
    fetchHealth, fetchLoss, fetchRps, fetchErrorRate, fetchLatencyP50, fetchLatencyP95, fetchMps, fetchElo, fetchThroughput, fetchTrainingSeries, fetchUtilization,
    selectScalar, selectSeries, selectSeriesVm, selectHealth,
    pollTargets,
  }
})

// Reduce memory pressure by thinning very dense series
const MAX_POINTS = 10000
const MAX_SERIES_KEYS = 16
function prune(resp: TimeseriesResponse): TimeseriesResponse {
  const thin = <T>(arr: T[], limit: number) => {
    if (!arr) return arr
    if (arr.length <= limit) return arr
    const step = Math.ceil(arr.length / limit)
    const res: T[] = []
    for (let i = 0; i < arr.length; i += step) res.push(arr[i])
    return res
  }
  return {
    range: resp.range,
    series: resp.series.map(s => ({ ...s, points: thin(s.points, MAX_POINTS) }))
  }
}

function evictSeries() {
  // @ts-ignore - access inside store module scope
  const s = (useMetricsStore as any)?.stores?.metrics // not available; fallback to global vars captured
}
