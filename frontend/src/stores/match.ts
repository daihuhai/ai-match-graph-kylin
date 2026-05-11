import { defineStore } from 'pinia'
import { getDataStorage } from '@/utils/storage'
import type { MatchFeedback, MatchHistoryItem, MatchListItem } from '@/types/match'
import { useAuthStore } from '@/stores/auth'

export interface MatchState {
  favorites: Record<string, string[]>
  history: MatchHistoryItem[]
  feedbacks: MatchFeedback[]
}

const STORAGE_KEY = 'aimap.match'

const getUserKey = () => {
  const auth = useAuthStore()
  return `${auth.userType ?? 'ANON'}:${auth.userId || 'anon'}`
}

export const useMatchStore = defineStore('match', {
  state: (): MatchState => ({
    favorites: {},
    history: [],
    feedbacks: [],
  }),
  getters: {
    favoriteSet: (s) => (userKey: string) => new Set(s.favorites[userKey] ?? []),
    feedbackByRecord: (s) => (userKey: string, recordId: string) =>
      s.feedbacks.filter((f) => f.userKey === userKey && f.recordId === recordId).sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    historyByUser: (s) => (userKey: string) => s.history.filter((h) => h.userKey === userKey).sort((a, b) => b.viewedAt.localeCompare(a.viewedAt)),
  },
  actions: {
    hydrate() {
      const storage = getDataStorage()
      const raw = storage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const data = JSON.parse(raw) as Partial<MatchState>
        this.favorites = data.favorites && typeof data.favorites === 'object' ? (data.favorites as Record<string, string[]>) : {}
        this.history = Array.isArray(data.history) ? (data.history as MatchHistoryItem[]) : []
        this.feedbacks = Array.isArray(data.feedbacks) ? (data.feedbacks as MatchFeedback[]) : []
      } catch {
        storage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      const storage = getDataStorage()
      storage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          favorites: this.favorites,
          history: this.history,
          feedbacks: this.feedbacks,
        }),
      )
    },
    isFavorite(recordId: string) {
      const userKey = getUserKey()
      return this.favoriteSet(userKey).has(recordId)
    },
    toggleFavorite(recordId: string) {
      const userKey = getUserKey()
      const list = this.favorites[userKey] ?? []
      const idx = list.indexOf(recordId)
      if (idx >= 0) list.splice(idx, 1)
      else list.unshift(recordId)
      this.favorites[userKey] = list
      this.persist()
    },
    addHistory(item: MatchListItem, side: 'PERSON' | 'COMPANY') {
      const userKey = getUserKey()
      const viewedAt = new Date().toISOString()
      const next: MatchHistoryItem = { ...item, userKey, viewedAt, side }
      const idx = this.history.findIndex((h) => h.userKey === userKey && h.recordId === item.recordId)
      if (idx >= 0) this.history.splice(idx, 1)
      this.history.unshift(next)
      this.history = this.history.slice(0, 200)
      this.persist()
    },
    addFeedback(payload: { recordId: string; rating: 1 | 2 | 3 | 4 | 5; tags: string[]; comment: string }) {
      const userKey = getUserKey()
      const fb: MatchFeedback = {
        id: `fb-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        recordId: payload.recordId,
        userKey,
        rating: payload.rating,
        tags: payload.tags,
        comment: payload.comment,
        createdAt: new Date().toISOString(),
      }
      this.feedbacks.unshift(fb)
      this.feedbacks = this.feedbacks.slice(0, 500)
      this.persist()
      return fb
    },
  },
})

