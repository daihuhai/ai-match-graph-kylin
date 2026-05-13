import { defineStore } from 'pinia'
import type { DocFileVO, DocStatus, ParseResultVO } from '@/types/document'
import { getDataStorage } from '@/utils/storage'

export interface DocumentState {
  docs: DocFileVO[]
  results: Record<string, ParseResultVO>
}

const STORAGE_KEY = 'aimap.docs'

export const useDocumentStore = defineStore('document', {
  state: (): DocumentState => ({
    docs: [],
    results: {},
  }),
  actions: {
    hydrate() {
      const storage = getDataStorage()
      const raw = storage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const data = JSON.parse(raw) as Partial<DocumentState>
        this.docs = Array.isArray(data.docs) ? (data.docs as DocFileVO[]) : []
        this.results = data.results && typeof data.results === 'object' ? (data.results as Record<string, ParseResultVO>) : {}
      } catch {
        storage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      const storage = getDataStorage()
      storage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          docs: this.docs,
          results: this.results,
        }),
      )
    },
    addDoc(doc: DocFileVO) {
      const idx = this.docs.findIndex((d) => d.id === doc.id)
      if (idx >= 0) this.docs.splice(idx, 1, doc)
      else this.docs.unshift(doc)
      this.persist()
    },
    updateStatus(docId: string, status: DocStatus) {
      const d = this.docs.find((x) => x.id === docId)
      if (!d) return
      d.status = status
      this.persist()
    },
    setResult(docId: string, result: ParseResultVO) {
      this.results[docId] = result
      this.persist()
    },
    setDeliveredCompanyAccounts(docId: string, companyAccounts: string[]) {
      const current = this.results[docId]
      if (!current) return
      this.results[docId] = {
        ...current,
        deliveredCompanyAccounts: [...companyAccounts],
      }
      this.persist()
    },
    clearResult(docId: string) {
      if (docId in this.results) {
        delete this.results[docId]
        this.persist()
      }
    },
    removeDoc(docId: string) {
      this.docs = this.docs.filter((d) => d.id !== docId)
      this.clearResult(docId)
      this.persist()
    },
  },
})
