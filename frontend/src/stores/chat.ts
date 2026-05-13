import { defineStore } from 'pinia'
import { getDataStorage } from '@/utils/storage'

export type ChatRole = 'PERSON' | 'COMPANY'

export interface ChatParticipant {
  role: ChatRole
  account: string
  name: string
}

export interface ChatMessage {
  id: string
  threadId: string
  senderRole: ChatRole
  senderAccount: string
  senderName: string
  text: string
  createdAt: string
  readBy: string[]
}

export interface ChatThread {
  id: string
  personAccount: string
  companyAccount: string
  personName: string
  companyName: string
  contextRecordId: string
  contextTitle: string
  contextOrg: string
  updatedAt: string
}

export interface ChatState {
  threads: ChatThread[]
  messages: ChatMessage[]
}

const STORAGE_KEY = 'aimap.chat'

const threadIdFor = (personAccount: string, companyAccount: string, contextRecordId: string) =>
  `chat:${personAccount.trim().toLowerCase()}::${companyAccount.trim().toLowerCase()}::${contextRecordId.trim().toLowerCase()}`

const readerKeyFor = (role: ChatRole, account: string) => `${role}:${account.trim().toLowerCase()}`

const migrateState = (input: Partial<ChatState>): ChatState => {
  const rawThreads = Array.isArray(input.threads) ? (input.threads as ChatThread[]) : []
  const rawMessages = Array.isArray(input.messages) ? (input.messages as ChatMessage[]) : []
  const threadIdMap = new Map<string, string>()

  const threads = rawThreads.map((thread) => {
    const nextId = threadIdFor(thread.personAccount, thread.companyAccount, thread.contextRecordId || 'legacy')
    threadIdMap.set(thread.id, nextId)
    return { ...thread, id: nextId }
  })

  const messages = rawMessages.map((message) => ({
    ...message,
    threadId: threadIdMap.get(message.threadId) ?? message.threadId,
  }))

  return { threads, messages }
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    threads: [],
    messages: [],
  }),
  getters: {
    threadById: (s) => (threadId: string) => s.threads.find((x) => x.id === threadId) ?? null,
    messagesByThread: (s) => (threadId: string) =>
      s.messages.filter((x) => x.threadId === threadId).sort((a, b) => a.createdAt.localeCompare(b.createdAt)),
    unreadCount:
      (s) =>
      (threadId: string, role: ChatRole, account: string) =>
        s.messages.filter((x) => x.threadId === threadId && !x.readBy.includes(readerKeyFor(role, account))).length,
  },
  actions: {
    hydrate() {
      const storage = getDataStorage()
      const raw = storage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const data = migrateState(JSON.parse(raw) as Partial<ChatState>)
        this.threads = data.threads
        this.messages = data.messages
        this.persist()
      } catch {
        storage.removeItem(STORAGE_KEY)
      }
    },
    persist() {
      const storage = getDataStorage()
      storage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          threads: this.threads,
          messages: this.messages,
        }),
      )
    },
    ensureThread(payload: {
      personAccount: string
      personName: string
      companyAccount: string
      companyName: string
      contextRecordId: string
      contextTitle: string
      contextOrg: string
    }) {
      const threadId = threadIdFor(payload.personAccount, payload.companyAccount, payload.contextRecordId)
      const now = new Date().toISOString()
      const next: ChatThread = {
        id: threadId,
        personAccount: payload.personAccount,
        companyAccount: payload.companyAccount,
        personName: payload.personName,
        companyName: payload.companyName,
        contextRecordId: payload.contextRecordId,
        contextTitle: payload.contextTitle,
        contextOrg: payload.contextOrg,
        updatedAt: now,
      }
      const idx = this.threads.findIndex((x) => x.id === threadId)
      if (idx >= 0) this.threads.splice(idx, 1, { ...this.threads[idx], ...next })
      else this.threads.unshift(next)
      this.threads.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
      this.persist()
      return this.threads.find((x) => x.id === threadId) ?? next
    },
    sendMessage(payload: {
      threadId: string
      senderRole: ChatRole
      senderAccount: string
      senderName: string
      text: string
    }) {
      const text = payload.text.trim()
      if (!text) return null
      const createdAt = new Date().toISOString()
      const msg: ChatMessage = {
        id: `msg-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
        threadId: payload.threadId,
        senderRole: payload.senderRole,
        senderAccount: payload.senderAccount,
        senderName: payload.senderName,
        text,
        createdAt,
        readBy: [readerKeyFor(payload.senderRole, payload.senderAccount)],
      }
      this.messages.push(msg)
      const thread = this.threads.find((x) => x.id === payload.threadId)
      if (thread) thread.updatedAt = createdAt
      this.threads.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
      this.messages = this.messages.slice(-1000)
      this.persist()
      return msg
    },
    markThreadRead(threadId: string, role: ChatRole, account: string) {
      const readerKey = readerKeyFor(role, account)
      let changed = false
      for (const msg of this.messages) {
        if (msg.threadId !== threadId) continue
        if (msg.readBy.includes(readerKey)) continue
        msg.readBy.push(readerKey)
        changed = true
      }
      if (changed) this.persist()
    },
  },
})
