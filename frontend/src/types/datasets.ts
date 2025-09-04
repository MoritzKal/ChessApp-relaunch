import type { ISODate, UUID } from './common'
import { z } from 'zod'

export interface Dataset {
  id: UUID
  name: string
  version: string
  sizeRows: number
  locationUri: string
  createdAt: ISODate
}

export interface DatasetSummary {
  rows: number
  sizeBytes: number
  classes?: number
}

export interface DatasetVersionInfo {
  modelVersion?: string
  createdAt: ISODate
  metrics?: Record<string, number>
}

// zod parsers (thin, optional)
export const zDataset = z.object({
  id: z.string(),
  name: z.string(),
  version: z.string(),
  sizeRows: z.number(),
  locationUri: z.string(),
  createdAt: z.string()
})

export const zDatasetSummary = z.object({
  rows: z.number(),
  sizeBytes: z.number(),
  classes: z.number().optional()
})

export const zDatasetVersionInfo = z.object({
  modelVersion: z.string().optional(),
  createdAt: z.string(),
  metrics: z.record(z.number()).optional()
})

