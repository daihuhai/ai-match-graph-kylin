import { defineStore } from 'pinia'
import { getDataStorage } from '@/utils/storage'
import { useAuthStore } from '@/stores/auth'

export type AuditResult = 'OK' | 'FAIL'

export interface AuditLogRow {
  id: string
  time: string
  userKey: string
  module: string
  result: AuditResult
  detail: Record<string, unknown>
}

export interface AuditState {
  logs: AuditLogRow[]
}

const STORAGE_KEY = 'aimap.audit'

const getUserKey = () => {
  const auth = useAuthStore()
  return `${auth.userType ?? 'ANON'}:${auth.userId || 'anon'}`
}

export const useAuditStore = defineStore('audit', {
  state: (): AuditState => ({
    logs: [],
  }),
  actions: {
    hydrate() {
      const storage = getDataStorage()
      const raw = storage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const data = JSON.parse(raw) as Partial<AuditState>
        this.logs = Array.isArray(data.logs) ? (data.logs as AuditLogRow[]) : []
      } catch {
        storage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      getDataStorage().setItem(STORAGE_KEY, JSON.stringify({ logs: this.logs }))
    },
    add(payload: { module: string; result: AuditResult; detail?: Record<string, unknown>; userKey?: string }) {
      const row: AuditLogRow = {
        id: `log-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        time: new Date().toISOString(),
        userKey: payload.userKey || getUserKey(),
        module: payload.module,
        result: payload.result,
        detail: payload.detail || {},
      }
      this.logs.unshift(row)
      this.logs = this.logs.slice(0, 2000)
      this.persist()
      return row
    },
    clear() {
      this.logs = []
      this.persist()
    },
  },
})

