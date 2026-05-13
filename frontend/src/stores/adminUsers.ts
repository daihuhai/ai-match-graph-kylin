import { defineStore } from 'pinia'
import {
  createAdminUser,
  deleteAdminUser,
  listAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserStatus,
  type AdminUserRow,
  type AdminUserStatus,
  type AdminUserType,
} from '@/api/adminUsers'

export type { AdminUserRow, AdminUserStatus, AdminUserType }

export interface AdminUsersState {
  users: AdminUserRow[]
  loading: boolean
}

export const useAdminUsersStore = defineStore('adminUsers', {
  state: (): AdminUsersState => ({
    users: [],
    loading: false,
  }),
  actions: {
    async fetchUsers() {
      this.loading = true
      try {
        this.users = await listAdminUsers()
      } finally {
        this.loading = false
      }
    },
    async upsert(user: {
      id?: number
      account: string
      userType: AdminUserType
      status: AdminUserStatus
      phone?: string
      email?: string
    }) {
      const row = user.id
        ? await updateAdminUser(user.id, user)
        : await createAdminUser(user)
      const idx = this.users.findIndex((u) => u.id === row.id)
      if (idx >= 0) this.users.splice(idx, 1, row)
      else this.users.push(row)
      this.users.sort((a, b) => a.id - b.id)
      return row
    },
    async setStatus(id: number, status: AdminUserStatus) {
      const row = await updateAdminUserStatus(id, status)
      const idx = this.users.findIndex((u) => u.id === id)
      if (idx >= 0) this.users.splice(idx, 1, row)
      return row
    },
    async remove(id: number) {
      await deleteAdminUser(id)
      this.users = this.users.filter((x) => x.id !== id)
    },
    async resetPassword(id: number) {
      return resetAdminUserPassword(id)
    },
  },
})
