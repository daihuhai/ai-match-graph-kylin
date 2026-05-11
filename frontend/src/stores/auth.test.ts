import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('persists and hydrates', () => {
    const a1 = useAuthStore()
    a1.setAuth({ token: 't', userType: 'PERSON', userId: 'u', permissions: ['P1'] })

    setActivePinia(createPinia())
    const a2 = useAuthStore()
    a2.hydrate()

    expect(a2.token).toBe('t')
    expect(a2.userType).toBe('PERSON')
    expect(a2.userId).toBe('u')
    expect(a2.permissions).toEqual(['P1'])
  })
})
