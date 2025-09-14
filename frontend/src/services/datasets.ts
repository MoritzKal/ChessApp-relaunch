import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { api } from '@/lib/api'
import type { Dataset, DatasetSummary } from '@/types/datasets'
import { zDataset, zDatasetSummary } from '@/types/datasets'
import type { CountResponse } from '@/types/common'
import { apiGet } from '@/lib/api'

// List endpoint returns DatasetListDto: { items: DatasetListItemDto[], nextOffset?: number }
// We normalize items for the table view to include sizeRows, sizeBytes, versions, updatedAt.
export async function listDatasets(params?: { limit?: number; offset?: number; sort?: string; q?: string }): Promise<any[]> {
  const res = await api.get(ep.datasets.list(params))
  const data = (res.data as any) || {}
  const items: any[] = Array.isArray(data.items) ? data.items : Array.isArray(data) ? data : []
  return items.map((it: any) => ({
    id: it.id,
    name: it.name,
    sizeRows: it.rows ?? it.sizeRows ?? 0,
    sizeBytes: it.sizeBytes ?? 0,
    versions: it.versions ?? { count: 0, latest: undefined },
    version: it.versions?.latest ?? it.version,
    updatedAt: it.updatedAt,
    createdAt: it.createdAt ?? it.updatedAt,
  }))
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
  const data = (res.data as any) || {}
  return Array.isArray(data.items) ? data.items : Array.isArray(data) ? data : []
}

// Schema & Stats
export interface DatasetSchemaRow { name: string; dtype: string; nullPct?: number; uniquePct?: number; min?: number | string; max?: number | string }
export async function getDatasetSchema(id: string, version?: string): Promise<DatasetSchemaRow[]> {
  const res = await api.get(ep.datasets.schema(id, { version }))
  const data = (res.data as any) || {}
  const cols = Array.isArray(data.columns) ? data.columns : Array.isArray(data.items) ? data.items : []
  return cols.map((c: any) => ({
    name: c.name,
    dtype: c.dtype,
    nullPct: c.nullPct ?? c.null_pct ?? 0,
    uniquePct: c.uniquePct ?? c.unique_pct ?? 0,
    min: c.min,
    max: c.max,
  }))
}

// Sample (pageable)
export interface DatasetSampleResponse { rows: Record<string, unknown>[]; nextCursor?: string | null }
export async function getDatasetSample(id: string, q?: { limit?: number; cursor?: string; version?: string }): Promise<DatasetSampleResponse> {
  const res = await api.get(ep.datasets.sample(id, q))
  const data = (res.data as any) || {}
  const items = Array.isArray(data.items) ? data.items : data.rows ?? []
  return { rows: items as any[], nextCursor: data.nextCursor }
}

// Quality summary
export interface DatasetQuality { missingPct: number; outlierPct: number; duplicatePct: number }
export async function getDatasetQuality(id: string, version?: string): Promise<DatasetQuality> {
  return apiGet<DatasetQuality>(ep.datasets.quality(id, { version }))
}

// Ingest history
export interface IngestEvent { at: string; user: string; note?: string; version?: string }
export async function getIngestHistory(id: string): Promise<IngestEvent[]> {
  const res = await api.get(ep.datasets.ingestHistory(id))
  const data = (res.data as any) || {}
  return Array.isArray(data.items) ? data.items as IngestEvent[] : []
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
