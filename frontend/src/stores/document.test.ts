import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useDocumentStore } from '@/stores/document'

describe('document store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('persists docs and results', () => {
    const s1 = useDocumentStore()
    s1.addDoc({
      id: 'doc-1',
      fileName: 'a.pdf',
      fileType: 'PDF',
      docType: 'RESUME',
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00.000Z',
    })
    s1.setResult('doc-1', { docId: 'doc-1', status: 'DONE', resultJson: { ok: true } })

    setActivePinia(createPinia())
    const s2 = useDocumentStore()
    s2.hydrate()

    expect(s2.docs.length).toBe(1)
    expect(s2.docs[0].id).toBe('doc-1')
    expect(s2.results['doc-1'].status).toBe('DONE')
  })
})

