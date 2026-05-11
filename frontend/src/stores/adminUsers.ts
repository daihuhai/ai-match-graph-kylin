import { defineStore } from 'pinia'
import { getDataStorage } from '@/utils/storage'

export type AdminUserType = 'PERSON' | 'COMPANY' | 'ADMIN'
export type AdminUserStatus = 'ACTIVE' | 'DISABLED'

export interface AdminUserRow {
  id: string
  account: string
  userType: AdminUserType
  status: AdminUserStatus
  createdAt: string
  lastLoginAt?: string
}

export interface AdminUsersState {
  users: AdminUserRow[]
}

const STORAGE_KEY = 'aimap.admin.users'

export const useAdminUsersStore = defineStore('adminUsers', {
  state: (): AdminUsersState => ({
    users: [],
  }),
  actions: {
    hydrate() {
      const storage = getDataStorage()
      const raw = storage.getItem(STORAGE_KEY)
      if (!raw) {
        const now = new Date().toISOString()
        this.users = [
          { id: 'u-001', account: 'demo', userType: 'PERSON', status: 'ACTIVE', createdAt: now, lastLoginAt: now },
          { id: 'u-002', account: 'hr-demo', userType: 'COMPANY', status: 'ACTIVE', createdAt: now, lastLoginAt: now },
          { id: 'u-003', account: 'admin', userType: 'ADMIN', status: 'ACTIVE', createdAt: now, lastLoginAt: now },
        ]
        this.persist()
        return
      }
      try {
        const data = JSON.parse(raw) as Partial<AdminUsersState>
        this.users = Array.isArray(data.users) ? (data.users as AdminUserRow[]) : []
      } catch {
        storage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      getDataStorage().setItem(STORAGE_KEY, JSON.stringify({ users: this.users }))
    },
    upsert(user: Omit<AdminUserRow, 'id' | 'createdAt'> & Partial<Pick<AdminUserRow, 'id' | 'createdAt'>>) {
      const id = user.id || `u-${Date.now()}`
      const createdAt = user.createdAt || new Date().toISOString()
      const row: AdminUserRow = {
        id,
        account: user.account,
        userType: user.userType,
        status: user.status,
        createdAt,
        lastLoginAt: user.lastLoginAt,
      }
      const idx = this.users.findIndex((u) => u.id === id)
      if (idx >= 0) this.users.splice(idx, 1, row)
      else this.users.unshift(row)
      this.persist()
      return row
    },
    setStatus(id: string, status: AdminUserStatus) {
      const u = this.users.find((x) => x.id === id)
      if (!u) return
      u.status = status
      this.persist()
    },
    remove(id: string) {
      this.users = this.users.filter((x) => x.id !== id)
      this.persist()
    },
  },
})

