import type { ISODate, OkStatus } from './common'
import { z } from 'zod'

// Generic metric responses used by the dashboards

export interface ScalarMetric {
  value: number
  unit?: string
  at?: ISODate
}

export interface TimeseriesPoint { ts: ISODate; value: number }
export interface TimeseriesSeries { metric: string; points: TimeseriesPoint[]; unit?: string }
export interface TimeseriesResponse { series: TimeseriesSeries[]; range?: string }

export interface HealthAggregate {
  status: OkStatus
  ok: number
  warn: number
  crit: number
}

// Known metric keys used by dashboards
export type MetricKey =
  | 'loss'
  | 'val_acc'
  | 'rps'
  | 'error_rate'
  | 'throughput'
  | 'latency_p50'
  | 'latency_p95'
  | 'elo'
  | 'mps'

export type RangeKey = '24h' | '7d' | '30d' | string

export const zScalarMetric = z.object({ value: z.number(), unit: z.string().optional(), at: z.string().optional() })
export const zTimeseriesPoint = z.object({ ts: z.string(), value: z.number() })
export const zTimeseriesSeries = z.object({
  metric: z.string(),
  unit: z.string().optional(),
  points: z.array(zTimeseriesPoint)
})
export const zTimeseriesResponse = z.object({ series: z.array(zTimeseriesSeries), range: z.string().optional() })
export const zHealthAggregate = z.object({ status: z.enum(['ok','warn','crit']), ok: z.number(), warn: z.number(), crit: z.number() })
