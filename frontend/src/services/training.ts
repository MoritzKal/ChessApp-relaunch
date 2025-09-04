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
  const res = await api.post(ep.training.start(), payload)
  return res.data as { runId: string }
}

export async function getTraining(runId: string): Promise<TrainingRun> {
  return getParsed<TrainingRun>(ep.training.get(runId), zTrainingRun)
}

export async function listTrainingRuns(params?: { limit?: number; offset?: number; status?: string }): Promise<TrainingRun[]> {
  const res = await api.get(ep.training.runs(params))
  return (res.data as any[]).map((t) => zTrainingRun.parse(t))
}

export async function countTraining(q?: { status?: string }): Promise<number> {
  const res = await api.get<CountResponse>(ep.training.count(q))
  return (res.data?.count as number) ?? 0
}

// Artifacts listing + Hyperparameters
export interface TrainingArtifact { name: string; sizeBytes?: number; downloadUrl?: string }
export async function listArtifacts(runId: string): Promise<TrainingArtifact[]> {
  return apiGet<TrainingArtifact[]>(ep.training.artifacts(runId))
}

export interface HyperParamKV { key: string; value: string | number | boolean }
export async function getHyperparams(runId: string): Promise<Record<string, unknown> | HyperParamKV[]> {
  return apiGet<Record<string, unknown> | HyperParamKV[]>(ep.training.hyperparams(runId))
}

export async function controlTrainingRun(runId: string, action: 'pause' | 'stop'): Promise<{ ok: boolean }> {
  const res = await api.post(ep.training.control(runId), { action })
  return (res.data as any) ?? { ok: true }
}
