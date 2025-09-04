import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { zScalarMetric, zTimeseriesResponse, zHealthAggregate } from '@/types/metrics'
import type { ScalarMetric, TimeseriesResponse, HealthAggregate } from '@/types/metrics'

export const getHealth = (): Promise<HealthAggregate> => getParsed(ep.metrics.health(), zHealthAggregate)
export const getLoss = (range: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.loss(range), zTimeseriesResponse)
export const getRps = (range: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.rps(range), zTimeseriesResponse)
export const getErrorRate = (range: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.errorRate(range), zTimeseriesResponse)
export const getLatency = (p: 50 | 95 | 99): Promise<ScalarMetric> => getParsed(ep.metrics.latency(p), zScalarMetric)
export const getMps = (): Promise<ScalarMetric> => getParsed(ep.metrics.mps(), zScalarMetric)
export const getElo = (range: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.elo(range), zTimeseriesResponse)
export const getThroughput = (runId: string): Promise<ScalarMetric> => getParsed(ep.metrics.throughput(runId), zScalarMetric)
export const getTrainingMetric = (runId: string, m: string, range?: string): Promise<TimeseriesResponse> => getParsed(ep.metrics.trainingMetric(runId, m, range), zTimeseriesResponse)

