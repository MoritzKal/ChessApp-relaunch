import { Endpoints as ep } from '@/lib/endpoints'
import { api } from '@/lib/api'

export interface EvalCreate { baselineModelId: string; candidateModelId: string; suite: 'accuracy'|'topk'|'acpl'; notes?: string }

export async function createEvaluation(body: EvalCreate): Promise<{ evaluationId: string }> {
  const r = await api.post(ep.evaluations.create(), body)
  return r.data as any
}

export async function listEvaluations(limit = 20): Promise<any[]> {
  const r = await api.get(ep.evaluations.list({ limit }))
  const data = (r.data as any) || {}
  // Backend returns { items: [...] }
  const items: any[] = Array.isArray(data.items) ? data.items : Array.isArray(data) ? data : []
  return items
}

export async function getEvaluation(id: string): Promise<any> {
  const r = await api.get(ep.evaluations.get(id))
  return r.data as any
}
