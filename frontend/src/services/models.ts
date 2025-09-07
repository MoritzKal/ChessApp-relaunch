import { Endpoints as ep } from '@/lib/endpoints'
import { api } from '@/lib/api'

export interface ModelSummary { modelId: string; displayName?: string; tags?: string[] }

export async function listModels(): Promise<ModelSummary[]> {
  const res = await api.get(ep.models.list())
  return (res.data as any[]) as ModelSummary[]
}

