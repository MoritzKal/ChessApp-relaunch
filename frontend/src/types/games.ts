import type { ISODate } from './common'
import { z } from 'zod'

export type GameResult = 'WHITE' | 'BLACK' | 'DRAW' | 'ABORTED' | 'UNKNOWN'

export interface GameListItem {
  id: string
  endTime: ISODate
  timeControl: string
  result: GameResult
  whiteRating: number
  blackRating: number
}

export interface GameDetail extends GameListItem {
  pgnRaw: string
}

export interface GamePosition {
  ply: number
  fen: string
  sideToMove: 'WHITE' | 'BLACK'
}

export const zGameListItem = z.object({
  id: z.string(),
  endTime: z.string(),
  timeControl: z.string(),
  result: z.enum(['WHITE','BLACK','DRAW','ABORTED','UNKNOWN']).default('UNKNOWN'),
  whiteRating: z.number(),
  blackRating: z.number()
})

export const zGameDetail = zGameListItem.extend({ pgnRaw: z.string() })

export const zGamePosition = z.object({
  ply: z.number(),
  fen: z.string(),
  sideToMove: z.enum(['WHITE','BLACK'])
})

