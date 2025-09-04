import { Endpoints as ep } from '@/lib/endpoints'
import { apiGet } from '@/lib/api'

export interface LogLine { time: string; level: string; msg: string; [k: string]: unknown }

export const getTrainingLogs = (runId: string): Promise<LogLine[]> => apiGet<LogLine[]>(ep.logs.training(runId))

