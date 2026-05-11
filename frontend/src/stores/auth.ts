import { defineStore } from 'pinia'

export type UserType = 'PERSON' | 'COMPANY' | 'ADMIN'

export interface AuthState {
  token: string
  userType: UserType | null
  userId: string
  permissions: string[]
}

const STORAGE_KEY = 'aimap.auth'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: '',
    userType: null,
    userId: '',
    permissions: [],
  }),
  getters: {
    isLoggedIn: (s) => Boolean(s.token) && Boolean(s.userType),
  },
  actions: {
    hydrate() {
      const raw = sessionStorage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const data = JSON.parse(raw) as Partial<AuthState>
        this.token = data.token ?? ''
        this.userType = (data.userType ?? null) as UserType | null
        this.userId = data.userId ?? ''
        this.permissions = Array.isArray(data.permissions) ? data.permissions : []
      } catch {
        sessionStorage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          token: this.token,
          userType: this.userType,
          userId: this.userId,
          permissions: this.permissions,
        }),
      )
    },
    setAuth(payload: { token: string; userType: UserType; userId: string; permissions?: string[] }) {
      this.token = payload.token
      this.userType = payload.userType
      this.userId = payload.userId
      this.permissions = payload.permissions ?? []
      this.persist()
    },
    logout() {
      this.token = ''
      this.userType = null
      this.userId = ''
      this.permissions = []
      sessionStorage.removeItem(STORAGE_KEY)
    },
  },
})

