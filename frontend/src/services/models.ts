import { Endpoints as ep } from '@/lib/endpoints'
import { api } from '@/lib/api'

export interface ModelInfo { id: string; name?: string; status?: string }

export async function listModels(): Promise<ModelInfo[]> {
  const res = await api.get(ep.models.list())
  return (res.data as any[]) as ModelInfo[]
}

