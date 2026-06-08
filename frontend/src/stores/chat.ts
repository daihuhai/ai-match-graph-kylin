import { defineStore } from 'pinia'
import {
  ensureChatThread,
  fetchChatMessages,
  fetchChatThreads,
  markChatThreadRead,
  sendChatMessage,
  type EnsureThreadPayload,
} from '@/api/chat'

export type ChatRole = 'PERSON' | 'COMPANY'

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
  unreadCount?: number
}

export interface ChatState {
  threads: ChatThread[]
  messages: ChatMessage[]
  loadedThreadIds: string[]
  hydrating: boolean
}

const readerKeyFor = (role: ChatRole, account: string) => `${role}:${account.trim().toLowerCase()}`

const mergeThread = (threads: ChatThread[], next: ChatThread): ChatThread[] => {
  const idx = threads.findIndex((x) => x.id === next.id)
  if (idx >= 0) {
    const copy = [...threads]
    copy[idx] = { ...copy[idx], ...next }
    return copy.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
  }
  return [next, ...threads].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    threads: [],
    messages: [],
    loadedThreadIds: [],
    hydrating: false,
  }),
  getters: {
    threadById: (s) => (threadId: string) => s.threads.find((x) => x.id === threadId) ?? null,
    messagesByThread: (s) => (threadId: string) =>
      s.messages.filter((x) => x.threadId === threadId).sort((a, b) => a.createdAt.localeCompare(b.createdAt)),
    unreadCount:
      (s) =>
      (threadId: string, role: ChatRole, account: string) => {
        const thread = s.threads.find((x) => x.id === threadId)
        if (thread && typeof thread.unreadCount === 'number') return thread.unreadCount
        const key = readerKeyFor(role, account)
        return s.messages.filter((x) => x.threadId === threadId && !x.readBy.includes(key)).length
      },
  },
  actions: {
    async hydrate() {
      if (this.hydrating) return
      this.hydrating = true
      try {
        this.threads = await fetchChatThreads()
      } finally {
        this.hydrating = false
      }
    },
    async loadMessages(threadId: string) {
      const rows = await fetchChatMessages(threadId)
      this.messages = [...this.messages.filter((m) => m.threadId !== threadId), ...rows]
      if (!this.loadedThreadIds.includes(threadId)) {
        this.loadedThreadIds.push(threadId)
      }
    },
    async ensureThread(payload: EnsureThreadPayload): Promise<ChatThread> {
      const thread = await ensureChatThread(payload)
      this.threads = mergeThread(this.threads, thread)
      return thread
    },
    async sendMessage(payload: {
      threadId: string
      senderRole: ChatRole
      senderAccount: string
      senderName: string
      text: string
    }) {
      const text = payload.text.trim()
      if (!text) return null
      const msg = await sendChatMessage(payload.threadId, text)
      this.messages.push(msg)
      const thread = this.threads.find((x) => x.id === payload.threadId)
      if (thread) {
        thread.updatedAt = msg.createdAt
        thread.unreadCount = 0
        this.threads = mergeThread(this.threads, thread)
      }
      return msg
    },
    async markThreadRead(threadId: string, role: ChatRole, account: string) {
      const list = this.messagesByThread(threadId)
      const last = list[list.length - 1]
      await markChatThreadRead(threadId, last?.id)
      const readerKey = readerKeyFor(role, account)
      for (const msg of this.messages) {
        if (msg.threadId !== threadId) continue
        if (!msg.readBy.includes(readerKey)) msg.readBy.push(readerKey)
      }
      const thread = this.threads.find((x) => x.id === threadId)
      if (thread) {
        thread.unreadCount = 0
      }
    },
  },
})
