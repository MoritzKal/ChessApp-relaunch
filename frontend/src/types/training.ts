import type { ISODate, UUID } from './common'
import { z } from 'zod'

export type TrainingStatus = 'queued' | 'running' | 'succeeded' | 'failed'

export interface TrainingRun {
  runId: UUID
  status: TrainingStatus
  updatedAt: ISODate | null
  metrics?: { [k: string]: number }
  artifactUris?: { [k: string]: string }
}

export const zTrainingRun = z.object({
  runId: z.string(),
  status: z.enum(['queued','running','succeeded','failed']),
  updatedAt: z.string().nullable(),
  metrics: z.record(z.number()).optional(),
  artifactUris: z.record(z.string()).optional()
})

