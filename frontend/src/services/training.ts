import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { api } from '@/lib/api'
import type { TrainingRun } from '@/types/training'
import { zTrainingRun } from '@/types/training'
import type { CountResponse } from '@/types/common'
import { apiGet } from '@/lib/api'

// Flexible payload to support both preset-based and explicit hyperparams
export type CreateTrainingPayload = Record<string, unknown>
export async function createTraining(payload: CreateTrainingPayload): Promise<{ runId: string }> {
  // robust gegen verschachtelte Form-Objekte aus älteren Views
  const body = toApiTrainingPayload(payload)
  return api.post(ep.training.start(), body)
}

export async function getTraining(runId: string): Promise<TrainingRun> {
  return getParsed<TrainingRun>(ep.training.get(runId), zTrainingRun)
}

export async function listTrainingRuns(params?: { limit?: number; offset?: number; status?: string }): Promise<TrainingRun[]> {
  const res = await api.get(ep.training.runs(params))
  const data = res?.data as any
  const items = Array.isArray(data) ? data : (Array.isArray(data?.items) ? data.items : [])
  return (items as any[]).map((t) => zTrainingRun.parse(t))
}

/**
 * Akzeptiert sowohl flache als auch (legacy) verschachtelte Payloads
 * und normalisiert sie für das API-Contract.
 */
function toApiTrainingPayload(input: any) {
  const h = input?.hyperparams ?? {}
  const r = input?.resources ?? {}
  return {
    datasetId:         input?.datasetId ?? input?.dataset?.id,
    datasetVersion:    input?.datasetVersion ?? input?.version,
    modelId:           input?.modelId ?? input?.model?.id,
    epochs:            input?.epochs ?? h?.epochs,
    batchSize:         input?.batchSize ?? h?.batchSize,
    learningRate:      input?.learningRate ?? h?.learningRate,
    optimizer:         input?.optimizer ?? h?.optimizer,
    seed:              input?.seed ?? h?.seed,
    priority:          input?.priority ?? r?.priority,
    useGPU:            input?.useGPU ?? r?.useGPU,
  }
}

export async function countTraining(q?: { status?: string }): Promise<number> {
  const res = await api.get<CountResponse>(ep.training.count(q))
  return (res.data?.count as number) ?? 0
}

// Artifacts listing + Hyperparameters
export interface TrainingArtifact { name: string; sizeBytes?: number; downloadUrl?: string }
export async function listArtifacts(runId: string): Promise<TrainingArtifact[]> {
  const res = await apiGet<any>(ep.training.artifacts(runId))
  if (Array.isArray(res?.items)) return res.items as TrainingArtifact[]
  if (Array.isArray(res)) return res as TrainingArtifact[]
  return []
}

export interface HyperParamKV { key: string; value: string | number | boolean }
export async function getHyperparams(runId: string): Promise<Record<string, unknown> | HyperParamKV[]> {
  try {
    const res = await apiGet<any>(ep.training.hyperparams(runId))
    if (res && typeof res === 'object') return res as Record<string, unknown>
    if (Array.isArray(res?.items)) return res.items as HyperParamKV[]
    if (Array.isArray(res)) return res as HyperParamKV[]
  } catch { /* optional endpoint – ignore */ }
  return []
}

export async function controlTrainingRun(runId: string, action: 'pause' | 'stop'): Promise<{ ok: boolean }> {
  const res = await api.post(ep.training.control(runId), { action })
  return (res.data as any) ?? { ok: true }
}
