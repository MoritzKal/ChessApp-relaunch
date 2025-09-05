import { Endpoints as ep } from '@/lib/endpoints'
import { api } from '@/lib/api'

export interface AdminUser { id: string; username: string; roles: string[]; createdAt?: string }

export async function listUsers(): Promise<AdminUser[]> { const r = await api.get(ep.admin.users()); return r.data as any }
export async function updateUserRoles(id: string, roles: string[]): Promise<AdminUser> { const r = await api.patch(ep.admin.user(id), { roles }); return r.data as any }
