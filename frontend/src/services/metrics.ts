import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { zScalarMetric, zTimeseriesResponse } from '@/types/metrics'
import type { ScalarMetric, TimeseriesResponse, HealthAggregate } from '@/types/metrics'
import obsApi from '@/plugins/obsAxios'
import { api } from '@/lib/api'

// System health: now sourced from GET /v1/health
export const getHealth = async (): Promise<HealthAggregate> => {
  const res = await api.get(ep.health())
  const status = String((res.data as any)?.status || 'ok') as HealthAggregate['status']
  // Map simple health to aggregate for UI consumption
  return { status, ok: status === 'ok' ? 1 : 0, warn: status === 'warn' ? 1 : 0, crit: status === 'crit' ? 1 : 0 }
}

// ---- Observability proxy mapping (Prometheus/Loki) ----
// Helper: compute [start,end,step] for common ranges
function rangeWindow(range: string): { start: number; end: number; step: string } {
  const now = Math.floor(Date.now() / 1000)
  const presets: Record<string, { seconds: number; step: string }> = {
    '2h': { seconds: 2 * 3600, step: '30s' },
    '24h': { seconds: 24 * 3600, step: '2m' },
    '7d': { seconds: 7 * 24 * 3600, step: '1h' },
    '30d': { seconds: 30 * 24 * 3600, step: '2h' },
  }
  const def = presets[range] || { seconds: 24 * 3600, step: '2m' }
  return { start: now - def.seconds, end: now, step: def.step }
}

// Helper: map Prometheus range response -> TimeseriesResponse
function promToTimeseries(resp: any, fallbackMetric = 'metric'): TimeseriesResponse {
  const ok = resp && resp.status === 'success' && resp.data && Array.isArray(resp.data.result)
  if (!ok) return { series: [] }
  const series = resp.data.result.map((r: any) => {
    const m = r.metric || {}
    const name = m.__name__ || fallbackMetric
    const unit = undefined
    const points = Array.isArray(r.values)
      ? r.values.map((t: [number | string, string]) => ({ ts: new Date(Number(t[0]) * 1000).toISOString(), value: Number(t[1]) }))
      : []
    return { metric: name, unit, points }
  })
  return { series }
}

// Default queries (overridable via env)
const Q_RPS = (import.meta as any).env.VITE_PROMQL_RPS || 'sum(rate(http_requests_total[1m]))'
const Q_ERR = (import.meta as any).env.VITE_PROMQL_ERROR_RATE || 'sum(rate(http_requests_total{status=~"4..|5.."}[5m])) / sum(rate(http_requests_total[5m]))'
const Q_LOSS = (import.meta as any).env.VITE_PROMQL_LOSS || 'avg_over_time(ml_training_loss[5m])'
const Q_LAT_P = (p: 50 | 95 | 99) =>
  ((import.meta as any).env[`VITE_PROMQL_LATENCY_P${p}`]) || `histogram_quantile(${p/100}, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))`

async function promRange(query: string, range: string, fallbackMetric: string): Promise<TimeseriesResponse> {
  const { start, end, step } = rangeWindow(range)
  const res = await obsApi.get('/obs/prom/range', { params: { query, start, end, step } })
  return promToTimeseries(res.data, fallbackMetric)
}

// Timeseries tiles via Obs-Proxy (Prometheus)
export const getLoss = (range: string): Promise<TimeseriesResponse> => promRange(Q_LOSS, range, 'loss')
export const getRps = (range: string): Promise<TimeseriesResponse> => promRange(Q_RPS, range, 'rps')
export const getErrorRate = (range: string): Promise<TimeseriesResponse> => promRange(Q_ERR, range, 'error_rate')

// Latency as scalar using Prometheus instant query
export const getLatency = async (p: 50 | 95 | 99): Promise<ScalarMetric> => {
  const query = Q_LAT_P(p)
  const res = await obsApi.get('/obs/prom/query', { params: { query } })
  const ok = res.data && res.data.status === 'success' && res.data.data && Array.isArray(res.data.data.result)
  const first = ok && res.data.data.result[0]
  const val = first && first.value && Number(first.value[1])
  return { value: Number.isFinite(val) ? val : 0 }
}
export const getMps = (): Promise<ScalarMetric> => getParsed(ep.metrics.mps(), zScalarMetric)
export const getElo = (range: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.elo(range), zTimeseriesResponse)
export const getThroughput = (runId: string): Promise<ScalarMetric> => getParsed(ep.metrics.throughput(runId), zScalarMetric)
export const getTrainingMetric = (runId: string, m: string, range?: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.trainingMetric(runId, m, range), zTimeseriesResponse)
