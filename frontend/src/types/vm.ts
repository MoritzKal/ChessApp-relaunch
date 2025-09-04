// View-Model shapes for reusable renderers
export interface KpiVM {
  label: string
  value: number | string
  unit?: string
  badge?: string
  delta?: { value: number; direction: 'up' | 'down' | 'none' }
  ariaLabel?: string
  href?: string // deep-link target
}

export interface XY { x: number; y: number }
export interface ChartSeriesVM { label: string; data: XY[]; color?: string; type?: 'line' | 'area' }
export interface SeriesVM { series: ChartSeriesVM[]; range?: string }

export interface TableColumnVM { key: string; label: string; align?: 'start' | 'center' | 'end' }
export interface TableVM { columns: TableColumnVM[]; rows: Record<string, unknown>[]; total?: number }

