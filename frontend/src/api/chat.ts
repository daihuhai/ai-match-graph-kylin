import { http } from '@/api/http'
import type { ChatMessage, ChatThread } from '@/stores/chat'

export interface EnsureThreadPayload {
  personAccount: string
  personName: string
  companyAccount: string
  companyName: string
  contextRecordId: string
  contextTitle: string
  contextOrg: string
}

export async function fetchChatThreads(): Promise<ChatThread[]> {
  return http.get('/chat/threads')
}

export async function ensureChatThread(payload: EnsureThreadPayload): Promise<ChatThread> {
  return http.post('/chat/threads', payload)
}

export async function fetchChatMessages(threadId: string): Promise<ChatMessage[]> {
  return http.get(`/chat/threads/${encodeURIComponent(threadId)}/messages`)
}

export async function sendChatMessage(threadId: string, text: string): Promise<ChatMessage> {
  return http.post(`/chat/threads/${encodeURIComponent(threadId)}/messages`, { text })
}

export async function markChatThreadRead(threadId: string, lastMessageId?: string): Promise<void> {
  const body =
    lastMessageId && /^\d+$/.test(lastMessageId)
      ? { lastMessageId: Number(lastMessageId) }
      : undefined
  await http.post(`/chat/threads/${encodeURIComponent(threadId)}/read`, body ?? {})
}
