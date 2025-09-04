import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import { api } from '@/lib/api'
import type { Dataset, DatasetSummary } from '@/types/datasets'
import { zDataset, zDatasetSummary } from '@/types/datasets'
import type { CountResponse } from '@/types/common'

export async function listDatasets(params?: { limit?: number; offset?: number }): Promise<Dataset[]> {
  const res = await api.get(ep.datasets.list(params))
  return (res.data as any[]).map((d) => zDataset.parse(d))
}

export async function getDataset(id: string): Promise<Dataset> {
  return getParsed<Dataset>(ep.datasets.get(id), zDataset)
}

export async function countDatasets(): Promise<number> {
  const res = await api.get<CountResponse>(ep.datasets.count())
  return (res.data?.count as number) ?? 0
}

export async function getDatasetSummary(id: string): Promise<DatasetSummary> {
  return getParsed<DatasetSummary>(ep.datasets.summary(id), zDatasetSummary)
}

export async function getDatasetVersions(id: string): Promise<any[]> {
  const res = await api.get(ep.datasets.versions(id))
  return res.data as any[]
}

