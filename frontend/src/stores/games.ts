import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ApiError } from '@/types/common'
import type { GameDetail, GameListItem, GamePosition } from '@/types/games'
import { getGame, listGames, getPositions, onlineCount, recentGames } from '@/services/games'

export interface PollTarget { key: string; intervalMs: number; run: () => Promise<void> }

export const useGamesStore = defineStore('games', () => {
  const byId = ref(new Map<string, GameDetail>())
  const positionsByGame = ref(new Map<string, GamePosition[]>())
  const lists = ref(new Map<string, GameListItem[]>()) // key: username|params
  const online = ref<number | null>(null)

  const loading = ref(new Set<string>())
  const errors = ref(new Map<string, ApiError>())

  function setLoading(k: string, v: boolean) { v ? loading.value.add(k) : loading.value.delete(k) }
  function setError(k: string, e?: ApiError | null) { if (e) errors.value.set(k, e); else errors.value.delete(k) }

  function listKey(q: Record<string, unknown>) {
    return Object.entries(q).sort(([a],[b]) => a.localeCompare(b)).map(([k,v]) => `${k}=${v}`).join('&')
  }

  async function fetchGame(id: string) {
    const k = `get:${id}`
    try { setLoading(k, true); setError(k, null); byId.value.set(id, await getGame(id)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchPositions(id: string) {
    const k = `pos:${id}`
    try { setLoading(k, true); setError(k, null); positionsByGame.value.set(id, await getPositions(id)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchList(q: { username: string; limit?: number; offset?: number; result?: string; color?: string; since?: string }) {
    const key = `list:${listKey(q)}`
    try { setLoading(key, true); setError(key, null); lists.value.set(key, await listGames(q)) }
    catch (e: any) { setError(key, e) }
    finally { setLoading(key, false) }
  }

  async function fetchOnline() {
    const k = 'online'
    try { setLoading(k, true); setError(k, null); online.value = await onlineCount() }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  async function fetchRecent(limit = 50) {
    const k = `recent:${limit}`
    try { setLoading(k, true); setError(k, null); lists.value.set(k, await recentGames(limit)) }
    catch (e: any) { setError(k, e) }
    finally { setLoading(k, false) }
  }

  function selectGame(id: string) { return computed(() => byId.value.get(id) || null) }
  function selectPositions(id: string) { return computed(() => positionsByGame.value.get(id) || []) }
  function selectList(q: { username?: string; limit?: number; offset?: number; result?: string; color?: string; since?: string }) {
    const key = q.username ? `list:${listKey(q as Record<string, unknown>)}"` : `recent:${q.limit ?? 50}`
    return computed(() => lists.value.get(key) || [])
  }
  const selectOnline = computed(() => online.value)

  const pollTargets = computed<PollTarget[]>(() => ([
    { key: 'games.online', intervalMs: 2000, run: fetchOnline },
  ]))

  return {
    byId, positionsByGame, lists, online, loading, errors,
    fetchGame, fetchPositions, fetchList, fetchOnline, fetchRecent,
    selectGame, selectPositions, selectList, selectOnline,
    pollTargets
  }
})

