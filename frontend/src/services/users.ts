import { Endpoints as ep } from '@/lib/endpoints'
import { api } from '@/lib/api'

export interface Me { username: string; createdAt?: string; roles?: string[] }
export interface Token { id: string; createdAt?: string; lastUsedAt?: string; masked?: string }

export async function getMe(): Promise<Me> { const r = await api.get(ep.users.me()); return r.data as Me }
export async function getPrefs(): Promise<Record<string, unknown>> { const r = await api.get(ep.users.prefs()); return r.data as any }
export async function putPrefs(p: Record<string, unknown>): Promise<void> { await api.put(ep.users.prefs(), p) }
export async function listTokens(): Promise<Token[]> { const r = await api.get(ep.users.tokens()); return (r.data as any[]) as Token[] }
export async function createToken(): Promise<Token> { const r = await api.post(ep.users.tokens(), {}); return r.data as Token }
export async function revokeToken(id: string): Promise<void> { await api.delete(ep.users.token(id)) }

