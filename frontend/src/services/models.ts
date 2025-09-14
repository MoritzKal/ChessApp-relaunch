import { Endpoints as ep } from '@/lib/endpoints'
import { api } from '@/lib/api'

export interface ModelSummary { modelId: string; displayName?: string; tags?: string[] }
export interface ModelVersionSummary { modelVersion: string; createdAt?: string; metrics?: Record<string, number> }

export async function listModels(): Promise<ModelSummary[]> {
  const res = await api.get(ep.models.list())
  return (res.data as any[]) as ModelSummary[]
}

export async function listModelVersions(modelId: string): Promise<ModelVersionSummary[]> {
  const res = await api.get(ep.models.versions(modelId))
  const data = (res.data as any) || {}
  return Array.isArray(data.items) ? data.items as ModelVersionSummary[] : Array.isArray(data) ? data as ModelVersionSummary[] : []
}

export async function loadModel(p: { modelId: string; modelVersion?: string | null; artifactUri?: string | null }) {
  // Backend expects { modelId, artifactUri? } (no modelVersion)
  const body: any = { modelId: p.modelId }
  if (p.modelVersion) body.modelVersion = p.modelVersion
  if (p.artifactUri) body.artifactUri = p.artifactUri
  return api.post(ep.models.load(), body)
}
