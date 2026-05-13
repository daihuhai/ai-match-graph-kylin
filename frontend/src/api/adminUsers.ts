import { http } from '@/api/http'

export type AdminUserType = 'PERSON' | 'COMPANY' | 'ADMIN'
export type AdminUserStatus = 'ACTIVE' | 'DISABLED'

export interface AdminUserRow {
  id: number
  account: string
  userType: AdminUserType
  status: AdminUserStatus
  phone?: string
  email?: string
  createdAt?: string
  lastLoginAt?: string
}

export async function listAdminUsers(): Promise<AdminUserRow[]> {
  return http.get('/admin/users')
}

export async function createAdminUser(payload: {
  account: string
  userType: AdminUserType
  status: AdminUserStatus
  phone?: string
  email?: string
}): Promise<AdminUserRow> {
  return http.post('/admin/users', payload)
}

export async function updateAdminUser(
  id: number,
  payload: {
    account: string
    userType: AdminUserType
    status: AdminUserStatus
    phone?: string
    email?: string
  },
): Promise<AdminUserRow> {
  return http.put(`/admin/users/${id}`, payload)
}

export async function updateAdminUserStatus(id: number, status: AdminUserStatus): Promise<AdminUserRow> {
  return http.put(`/admin/users/${id}/status`, { status })
}

export async function resetAdminUserPassword(id: number): Promise<{ success: boolean; defaultPassword: string }> {
  return http.post(`/admin/users/${id}/reset-password`)
}

export async function deleteAdminUser(id: number): Promise<{ success: boolean }> {
  return http.delete(`/admin/users/${id}`)
}
