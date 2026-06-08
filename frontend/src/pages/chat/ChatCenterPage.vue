<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore, type ChatRole } from '@/stores/chat'
import { useAuditStore } from '@/stores/audit'
import AppEmpty from '@/components/AppEmpty.vue'

const auth = useAuthStore()
const chatStore = useChatStore()
const audit = useAuditStore()

const currentRole = computed(() => (auth.userType === 'COMPANY' ? 'COMPANY' : 'PERSON') as ChatRole)
const currentAccount = computed(() => auth.userId.trim())
const draft = ref('')
const activeThreadId = ref('')
const messagesRef = ref<HTMLElement>()
const loading = ref(false)
const sending = ref(false)

const myThreads = computed(() =>
  [...chatStore.threads].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
)

const activeThread = computed(() => myThreads.value.find((x) => x.id === activeThreadId.value) ?? myThreads.value[0] ?? null)
const activeMessages = computed(() => (activeThread.value ? chatStore.messagesByThread(activeThread.value.id) : []))

const counterpartName = computed(() => {
  if (!activeThread.value) return ''
  return currentRole.value === 'COMPANY' ? activeThread.value.personName : activeThread.value.companyName
})

const counterpartRoleLabel = computed(() => (currentRole.value === 'COMPANY' ? '个人用户' : '企业招聘方'))

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

const unreadFor = (threadId: string) => {
  if (!currentAccount.value) return 0
  return chatStore.unreadCount(threadId, currentRole.value, currentAccount.value)
}

const unreadTextFor = (threadId: string) => {
  const count = unreadFor(threadId)
  if (!count) return ''
  return `${count} 条新消息`
}

const syncRead = async () => {
  if (!activeThread.value || !currentAccount.value) return
  try {
    await chatStore.markThreadRead(activeThread.value.id, currentRole.value, currentAccount.value)
  } catch (e: any) {
    ElMessage.error(e?.message || '标记已读失败')
  }
}

const scrollToBottom = async () => {
  await nextTick()
  const el = messagesRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const openThread = async (threadId: string) => {
  activeThreadId.value = threadId
  try {
    await chatStore.loadMessages(threadId)
    await syncRead()
    audit.hydrate()
    audit.add({ module: 'chat.center.open', result: 'OK', detail: { threadId, side: currentRole.value } })
    await scrollToBottom()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载消息失败')
  }
}

const sendMessage = async () => {
  if (!activeThread.value || !draft.value.trim() || !currentAccount.value) return
  sending.value = true
  try {
    const senderName = currentRole.value === 'COMPANY' ? activeThread.value.companyName : activeThread.value.personName
    await chatStore.sendMessage({
      threadId: activeThread.value.id,
      senderRole: currentRole.value,
      senderAccount: currentAccount.value,
      senderName,
      text: draft.value,
    })
    draft.value = ''
    await syncRead()
    audit.hydrate()
    audit.add({ module: 'chat.center.send', result: 'OK', detail: { threadId: activeThread.value.id, side: currentRole.value } })
    await scrollToBottom()
  } catch (e: any) {
    ElMessage.error(e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

watch(
  myThreads,
  async (threads) => {
    if (!threads.length) {
      activeThreadId.value = ''
      return
    }
    if (!threads.some((x) => x.id === activeThreadId.value)) {
      await openThread(threads[0].id)
    }
  },
  { immediate: false },
)

watch(
  () => activeMessages.value.length,
  async () => {
    await scrollToBottom()
  },
)

onMounted(async () => {
  if (!currentAccount.value) {
    ElMessage.warning('当前登录信息缺失，消息中心可能无法正常显示')
    return
  }
  loading.value = true
  try {
    await chatStore.hydrate()
    if (myThreads.value.length) {
      await openThread(myThreads.value[0].id)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载会话失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-4" v-loading="loading">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">消息中心</div>
          <div class="mt-1 text-sm text-zinc-600">查看与你相关的沟通记录，继续与对方交流。</div>
        </div>
        <el-tag type="info">{{ myThreads.length }} 个会话</el-tag>
      </div>
    </el-card>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-[360px_minmax(0,1fr)]">
      <el-card shadow="never" body-class="p-0">
        <div class="border-b border-zinc-200 px-4 py-3 text-sm font-semibold text-zinc-800">会话列表</div>
        <AppEmpty v-if="myThreads.length === 0" description="还没有消息记录。" />
        <div v-else class="max-h-[72vh] overflow-y-auto">
          <button
            v-for="thread in myThreads"
            :key="thread.id"
            type="button"
            class="flex w-full items-start justify-between gap-3 border-b border-zinc-100 px-4 py-4 text-left transition hover:bg-zinc-50"
            :class="thread.id === activeThread?.id ? 'bg-emerald-50' : 'bg-white'"
            @click="openThread(thread.id)"
          >
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2">
                <div class="truncate text-sm font-semibold text-zinc-900">
                  {{ currentRole === 'COMPANY' ? thread.personName : thread.companyName }}
                </div>
                <el-tag v-if="unreadFor(thread.id)" size="small" type="danger">
                  {{ unreadTextFor(thread.id) }}
                </el-tag>
              </div>
              <div class="mt-1 truncate text-xs text-zinc-500">
                {{ thread.contextTitle }}<span v-if="thread.contextOrg"> · {{ thread.contextOrg }}</span>
              </div>
              <div class="mt-2 text-xs text-zinc-400">{{ fmt(thread.updatedAt) }}</div>
            </div>
            <el-badge v-if="unreadFor(thread.id)" :value="unreadFor(thread.id)" />
          </button>
        </div>
      </el-card>

      <el-card shadow="never" body-class="p-0">
        <AppEmpty v-if="!activeThread" description="选择一个会话开始查看消息。" />
        <div v-else class="flex h-[72vh] min-h-0 flex-col">
          <div class="border-b border-zinc-200 px-5 py-4">
            <div class="text-base font-semibold text-zinc-900">{{ counterpartName }}</div>
            <div class="mt-1 text-sm text-zinc-600">{{ counterpartRoleLabel }}</div>
            <div class="mt-1 text-xs text-zinc-500">
              {{ activeThread.contextTitle }}<span v-if="activeThread.contextOrg"> · {{ activeThread.contextOrg }}</span>
            </div>
          </div>

          <div ref="messagesRef" class="flex-1 overflow-y-auto bg-zinc-50 px-4 py-4">
            <div v-if="activeMessages.length === 0" class="flex h-full items-center justify-center text-sm text-zinc-500">
              先打个招呼吧。
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="msg in activeMessages"
                :key="msg.id"
                class="flex"
                :class="msg.senderRole === currentRole ? 'justify-end' : 'justify-start'"
              >
                <div
                  class="max-w-[80%] rounded-lg border px-4 py-3 text-sm leading-6 shadow-sm"
                  :class="msg.senderRole === currentRole ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-zinc-200 bg-white text-zinc-800'"
                >
                  <div class="text-xs opacity-80">{{ msg.senderName }}</div>
                  <div class="mt-1 whitespace-pre-wrap break-words">{{ msg.text }}</div>
                  <div class="mt-2 text-[11px] opacity-70">{{ fmt(msg.createdAt) }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="border-t border-zinc-200 bg-white px-4 py-4">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="4"
              resize="none"
              placeholder="输入消息内容"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
              <div class="text-xs text-zinc-500">Enter 发送</div>
              <button
                type="button"
                class="inline-flex h-10 w-28 items-center justify-center rounded-md bg-emerald-500 px-4 text-sm font-medium text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
                :disabled="!draft.trim() || sending"
                @click="sendMessage"
              >
                发送
              </button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>
