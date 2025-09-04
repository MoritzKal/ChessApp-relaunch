import { Endpoints as ep } from '@/lib/endpoints'
import { getParsed } from '@/lib/parse'
import type { GameDetail, GameListItem, GamePosition } from '@/types/games'
import { zGameDetail, zGameListItem, zGamePosition } from '@/types/games'
import { api } from '@/lib/api'

export async function listGames(q: { username: string; limit?: number; offset?: number; result?: string; color?: string; since?: string }): Promise<GameListItem[]> {
  const res = await api.get(ep.games.list(q))
  return (res.data as any[]).map((g) => zGameListItem.parse(g))
}

export const getGame = (id: string): Promise<GameDetail> => getParsed(ep.games.get(id), zGameDetail)
export async function getPositions(id: string): Promise<GamePosition[]> { return getParsed(ep.games.positions(id), zGamePosition.array()) }
export async function onlineCount(): Promise<number> { const r = await api.get<{ count: number }>(ep.games.onlineCount()); return r.data?.count ?? 0 }
export async function recentGames(limit = 50): Promise<GameListItem[]> {
  const res = await api.get(ep.games.recent(limit))
  return (res.data as any[]).map((g) => zGameListItem.parse(g))
}

