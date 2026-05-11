import { defineStore } from 'pinia'
import { getDataStorage } from '@/utils/storage'

export interface SkillRow {
  id: string
  name: string
  category: string
}

export interface SynonymRow {
  id: string
  term: string
  synonyms: string[]
}

export interface AdminDataState {
  skills: SkillRow[]
  synonyms: SynonymRow[]
}

const STORAGE_KEY = 'aimap.admin.data'

export const useAdminDataStore = defineStore('adminData', {
  state: (): AdminDataState => ({
    skills: [],
    synonyms: [],
  }),
  actions: {
    hydrate() {
      const storage = getDataStorage()
      const raw = storage.getItem(STORAGE_KEY)
      if (!raw) {
        this.skills = [
          { id: 'sk-001', name: 'Vue 3', category: '前端' },
          { id: 'sk-002', name: 'TypeScript', category: '前端' },
          { id: 'sk-003', name: 'Spring Boot', category: '后端' },
        ]
        this.synonyms = [
          { id: 'sy-001', term: 'Vue', synonyms: ['Vue3', 'Vue.js'] },
          { id: 'sy-002', term: 'TypeScript', synonyms: ['TS'] },
        ]
        this.persist()
        return
      }
      try {
        const data = JSON.parse(raw) as Partial<AdminDataState>
        this.skills = Array.isArray(data.skills) ? (data.skills as SkillRow[]) : []
        this.synonyms = Array.isArray(data.synonyms) ? (data.synonyms as SynonymRow[]) : []
      } catch {
        storage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      const storage = getDataStorage()
      storage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          skills: this.skills,
          synonyms: this.synonyms,
        }),
      )
    },
    upsertSkill(row: SkillRow) {
      const idx = this.skills.findIndex((x) => x.id === row.id)
      if (idx >= 0) this.skills.splice(idx, 1, row)
      else this.skills.unshift(row)
      this.persist()
    },
    removeSkill(id: string) {
      this.skills = this.skills.filter((x) => x.id !== id)
      this.persist()
    },
    upsertSynonym(row: SynonymRow) {
      const idx = this.synonyms.findIndex((x) => x.id === row.id)
      if (idx >= 0) this.synonyms.splice(idx, 1, row)
      else this.synonyms.unshift(row)
      this.persist()
    },
    removeSynonym(id: string) {
      this.synonyms = this.synonyms.filter((x) => x.id !== id)
      this.persist()
    },
  },
})
