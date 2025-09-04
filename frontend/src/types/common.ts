// Common, shared types across API domains
export type ISODate = string
export type UUID = string

export type OkStatus = 'ok' | 'warn' | 'crit'

export interface Page<T> {
  items: T[]
  total?: number
  limit?: number
  offset?: number
}

export interface ApiError {
  ok: false
  code: 'VALIDATION' | 'NOT_FOUND' | 'UNAUTHORIZED' | 'RATE_LIMIT' | 'INTERNAL'
  status: number
  message: string
  details?: Record<string, unknown>
}

export type ApiResult<T> = T | ApiError

export interface CountResponse { count: number }
export interface StatusCounts { [status: string]: number }
