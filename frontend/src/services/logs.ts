import { Endpoints as ep } from '@/lib/endpoints'
import { apiGet } from '@/lib/api'

export interface LogLine { time: string; level: string; msg: string; [k: string]: unknown }

async function unwrapItems<T = any>(path: string): Promise<T[]> {
  const res = await apiGet<any>(path)
  if (Array.isArray(res?.items)) return res.items as T[]
  if (Array.isArray(res)) return res as T[]
  return []
}

export const getTrainingLogs = (runId: string): Promise<LogLine[]> => unwrapItems<LogLine>(ep.logs.training(runId))
export const getAppLogs = (app: string, q?: { range?: string; limit?: number; direction?: 'backward'|'forward' }): Promise<LogLine[]> => unwrapItems<LogLine>(ep.logs.app(app, q))
