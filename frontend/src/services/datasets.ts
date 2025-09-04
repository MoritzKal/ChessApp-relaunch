import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { api } from '@/lib/api'
import type { Dataset, DatasetSummary } from '@/types/datasets'
import { zDataset, zDatasetSummary } from '@/types/datasets'
import type { CountResponse } from '@/types/common'
import { apiGet } from '@/lib/api'

export async function listDatasets(params?: { limit?: number; offset?: number; sort?: string }): Promise<Dataset[]> {
  const res = await api.get(ep.datasets.list(params))
  return (res.data as any[]).map((d) => zDataset.parse(d))
}

export async function getDataset(id: string): Promise<Dataset> {
  return getParsed<Dataset>(ep.datasets.get(id), zDataset)
}

export async function countDatasets(): Promise<number> {
  const res = await api.get<CountResponse>(ep.datasets.count())
  return (res.data?.count as number) ?? 0
}

export async function getDatasetSummary(id: string): Promise<DatasetSummary> {
  return getParsed<DatasetSummary>(ep.datasets.summary(id), zDatasetSummary)
}

export async function getDatasetVersions(id: string): Promise<any[]> {
  const res = await api.get(ep.datasets.versions(id))
  return res.data as any[]
}

// Schema & Stats
export interface DatasetSchemaRow { name: string; dtype: string; nullPct?: number; uniquePct?: number; min?: number | string; max?: number | string }
export async function getDatasetSchema(id: string): Promise<DatasetSchemaRow[]> {
  return apiGet<DatasetSchemaRow[]>(ep.datasets.schema(id))
}

// Sample (pageable)
export interface DatasetSampleResponse { rows: Record<string, unknown>[]; nextCursor?: string | null }
export async function getDatasetSample(id: string, q?: { limit?: number; cursor?: string }): Promise<DatasetSampleResponse> {
  return apiGet<DatasetSampleResponse>(ep.datasets.sample(id, q))
}

// Quality summary
export interface DatasetQuality { missingPct: number; outlierPct: number; duplicatePct: number }
export async function getDatasetQuality(id: string): Promise<DatasetQuality> {
  return apiGet<DatasetQuality>(ep.datasets.quality(id))
}

// Ingest history
export interface IngestEvent { at: string; user: string; note?: string; version?: string }
export async function getIngestHistory(id: string): Promise<IngestEvent[]> {
  return apiGet<IngestEvent[]>(ep.datasets.ingestHistory(id))
}
