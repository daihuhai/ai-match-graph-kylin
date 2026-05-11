import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useMatchStore } from '@/stores/match'

describe('match store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
  })

  it('persists favorites/history/feedbacks', () => {
    const auth = useAuthStore()
    auth.setAuth({ token: 't', userType: 'PERSON', userId: 'u' })

    const s1 = useMatchStore()
    s1.toggleFavorite('rec-job-001')
    s1.addHistory({ recordId: 'rec-job-001', title: 't', org: 'o', score: 80 }, 'PERSON')
    s1.addFeedback({ recordId: 'rec-job-001', rating: 5, tags: ['IRRELEVANT'], comment: 'ok' })

    setActivePinia(createPinia())
    const auth2 = useAuthStore()
    auth2.setAuth({ token: 't', userType: 'PERSON', userId: 'u' })

    const s2 = useMatchStore()
    s2.hydrate()

    const key = 'PERSON:u'
    expect(s2.favoriteSet(key).has('rec-job-001')).toBe(true)
    expect(s2.historyByUser(key)[0].recordId).toBe('rec-job-001')
    expect(s2.feedbackByRecord(key, 'rec-job-001').length).toBe(1)
  })
})

