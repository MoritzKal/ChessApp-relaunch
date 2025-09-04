import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { api } from '@/lib/api'
import type { Dataset, DatasetSummary } from '@/types/datasets'
import { zDataset, zDatasetSummary } from '@/types/datasets'
import type { CountResponse } from '@/types/common'
import { apiGet } from '@/lib/api'

export async function listDatasets(params?: { limit?: number; offset?: number; sort?: string; q?: string }): Promise<Dataset[]> {
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

// Ingest upload
export interface IngestStartResponse { runId: string; datasetId?: string }
export async function startIngest(file: File, opts?: { datasetId?: string; note?: string }): Promise<IngestStartResponse> {
  const fd = new FormData()
  fd.append('file', file)
  if (opts?.datasetId) fd.append('datasetId', opts.datasetId)
  if (opts?.note) fd.append('note', opts.note)
  const res = await api.post(ep.ingest.start(), fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  return res.data as IngestStartResponse
}

export interface IngestRun { runId: string; status: 'running'|'success'|'error'; datasetId?: string; error?: string }
export async function getIngestRun(runId: string): Promise<IngestRun> {
  const res = await api.get(ep.ingest.get(runId))
  const raw = res.data as any
  const s = String(raw?.status || '').toLowerCase()
  const map: Record<string, IngestRun['status']> = { pending: 'running', running: 'running', succeeded: 'success', success: 'success', failed: 'error', error: 'error' }
  const status = map[s] || 'running'
  return { runId: String(raw?.runId || runId), status, datasetId: raw?.datasetId, error: raw?.message }
}

// Export helper: returns absolute URL (respecting baseURL)
export function datasetExportUrl(id: string, q: { format: 'parquet'|'csv'|'pgn'; version?: string }): string {
  const base = (import.meta as any).env.VITE_API_BASE || '/api'
  return `${base}${ep.datasets.export(id, q)}`
}

// Chess.com integration
export interface ChesscomArchivesResponse { months: string[] }
export async function getChesscomArchives(user: string): Promise<ChesscomArchivesResponse> {
  return apiGet<ChesscomArchivesResponse>(ep.chesscom.archives(user))
}

export interface ChesscomArchiveMeta { count: number; timeControlDist?: Record<string, number> }
export async function getChesscomArchiveMeta(user: string, yyyymm: string): Promise<ChesscomArchiveMeta> {
  const [year, month] = yyyymm.split('-')
  return apiGet<ChesscomArchiveMeta>(ep.chesscom.archiveMeta(user, year, month))
}

export interface ChesscomImportBody { user: string; months: string[]; datasetId?: string; note?: string }
export interface ChesscomImportResponse { runId: string; status: 'queued'|'running'|'success'|'error' }
export async function importChesscom(body: ChesscomImportBody): Promise<ChesscomImportResponse> {
  const res = await api.post(ep.ingest.chesscom(), body)
  return res.data as ChesscomImportResponse
}
