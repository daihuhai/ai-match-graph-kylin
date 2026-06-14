<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore, type ChatRole } from '@/stores/chat'
import { useAuditStore } from '@/stores/audit'
import AppEmpty from '@/components/AppEmpty.vue'
import { Search, Send, MessageSquare, CheckCheck, Clock, User, Building2 } from 'lucide-vue-next'

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
const searchQuery = ref('')

const myThreads = computed(() =>
  [...chatStore.threads]
    .filter(t => {
      if (!searchQuery.value) return true
      const name = currentRole.value === 'COMPANY' ? t.personName : t.companyName
      const context = t.contextTitle || ''
      const org = t.contextOrg || ''
      return name.includes(searchQuery.value) || context.includes(searchQuery.value) || org.includes(searchQuery.value)
    })
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
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
  
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' })
}

const fmtFull = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const unreadFor = (threadId: string) => {
  if (!currentAccount.value) return 0
  return chatStore.unreadCount(threadId, currentRole.value, currentAccount.value)
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
    nextTick(() => {
      const inputEl = document.querySelector('.chat-input textarea') as HTMLTextAreaElement
      if (inputEl) inputEl.focus()
    })
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
  <div class="flex flex-col h-[calc(100vh-8rem)] min-h-[600px] space-y-4" v-loading="loading">
    <!-- Header -->
    <div class="flex items-center justify-between shrink-0">
      <div>
        <h1 class="text-2xl font-bold text-app-text tracking-tight">消息中心</h1>
        <p class="mt-1 text-sm text-app-subtext">查看与你相关的沟通记录，继续与对方交流。</p>
      </div>
      <div class="flex items-center gap-2 bg-indigo-50 px-3 py-1.5 rounded-full border border-indigo-100">
        <MessageSquare class="w-4 h-4 text-indigo-600" />
        <span class="text-sm font-medium text-indigo-700">{{ chatStore.threads.length }} 个会话</span>
      </div>
    </div>

    <!-- IM Layout -->
    <div class="flex-1 flex overflow-hidden rounded-2xl border border-app-border bg-app-panel shadow-sm">
      
      <!-- Sidebar (Thread List) -->
      <div class="w-80 border-r border-app-border flex flex-col bg-app-bg/50 shrink-0">
        <!-- Search -->
        <div class="p-4 border-b border-app-border bg-app-panel/50">
          <el-input
            v-model="searchQuery"
            placeholder="搜索联系人或职位..."
            :prefix-icon="Search"
            clearable
            class="!rounded-full"
          />
        </div>

        <!-- Thread List -->
        <div class="flex-1 overflow-y-auto custom-scrollbar">
          <AppEmpty v-if="myThreads.length === 0" description="没有找到相关的会话" class="mt-10" />
          <div v-else class="p-2 space-y-1">
            <button
              v-for="thread in myThreads"
              :key="thread.id"
              type="button"
              class="flex w-full items-start gap-3 rounded-xl p-3 text-left transition-all duration-200"
              :class="thread.id === activeThread?.id ? 'bg-indigo-50 shadow-sm ring-1 ring-indigo-100' : 'hover:bg-app-bg/80'"
              @click="openThread(thread.id)"
            >
              <!-- Avatar placeholder -->
              <div class="relative shrink-0 mt-0.5">
                <div class="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-indigo-100 to-slate-100 border border-app-border shadow-sm">
                  <User v-if="currentRole === 'COMPANY'" class="w-5 h-5 text-indigo-600" />
                  <Building2 v-else class="w-5 h-5 text-indigo-600" />
                </div>
                <div v-if="unreadFor(thread.id)" class="absolute -right-1 -top-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white shadow-sm ring-2 ring-white">
                  {{ unreadFor(thread.id) > 99 ? '99+' : unreadFor(thread.id) }}
                </div>
              </div>

              <!-- Content -->
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between">
                  <span class="truncate text-sm font-semibold" :class="thread.id === activeThread?.id ? 'text-indigo-950' : 'text-app-text'">
                    {{ currentRole === 'COMPANY' ? thread.personName : thread.companyName }}
                  </span>
                  <span class="shrink-0 text-xs text-app-subtext font-medium">{{ fmt(thread.updatedAt) }}</span>
                </div>
                
                <div class="mt-1 flex items-center gap-1.5 truncate text-xs" :class="thread.id === activeThread?.id ? 'text-indigo-700/80' : 'text-app-subtext'">
                  <span class="truncate">{{ thread.contextTitle }}</span>
                  <span v-if="thread.contextOrg" class="shrink-0 opacity-60">·</span>
                  <span v-if="thread.contextOrg" class="truncate">{{ thread.contextOrg }}</span>
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Main Chat Area -->
      <div class="flex-1 flex flex-col min-w-0 bg-app-bg/30 relative">
        <AppEmpty v-if="!activeThread" description="选择一个会话开始查看消息" class="m-auto" />
        <template v-else>
          <!-- Chat Header -->
          <div class="flex items-center justify-between border-b border-app-border bg-app-panel/80 backdrop-blur-md px-6 py-4 shrink-0 z-10">
            <div class="flex items-center gap-4">
              <div class="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-indigo-100 to-slate-100 border border-app-border shadow-sm">
                <User v-if="currentRole === 'COMPANY'" class="w-6 h-6 text-indigo-600" />
                <Building2 v-else class="w-6 h-6 text-indigo-600" />
              </div>
              <div>
                <div class="text-lg font-bold text-app-text flex items-center gap-2">
                  {{ counterpartName }}
                  <span class="px-2 py-0.5 rounded-md bg-app-bg text-app-text text-[10px] font-medium tracking-wide uppercase border border-app-border">
                    {{ counterpartRoleLabel }}
                  </span>
                </div>
                <div class="mt-0.5 text-sm text-app-subtext flex items-center gap-1.5">
                  <span class="inline-block w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]"></span>
                  沟通中 · {{ activeThread.contextTitle }}<span v-if="activeThread.contextOrg"> @ {{ activeThread.contextOrg }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Messages List -->
          <div ref="messagesRef" class="flex-1 overflow-y-auto px-6 py-6 custom-scrollbar">
            <div v-if="activeMessages.length === 0" class="flex h-full flex-col items-center justify-center text-app-subtext space-y-4">
              <div class="w-16 h-16 rounded-full bg-app-bg flex items-center justify-center border border-app-border">
                <MessageSquare class="w-8 h-8 text-slate-300" />
              </div>
              <p class="text-sm">这是你们沟通的开始，先打个招呼吧</p>
            </div>
            <div v-else class="space-y-6">
              <div
                v-for="(msg, index) in activeMessages"
                :key="msg.id"
                class="flex flex-col"
                :class="msg.senderRole === currentRole ? 'items-end' : 'items-start'"
              >
                <!-- Time separator (optional, simplified) -->
                <div v-if="index === 0 || new Date(msg.createdAt).getTime() - new Date(activeMessages[index-1].createdAt).getTime() > 1000 * 60 * 30" 
                     class="w-full flex justify-center mb-6 mt-2">
                  <span class="px-3 py-1 bg-app-bg/80 text-app-subtext text-xs font-medium rounded-full backdrop-blur-sm border border-app-border/50">
                    {{ fmtFull(msg.createdAt) }}
                  </span>
                </div>

                <div class="flex max-w-[75%] gap-3" :class="msg.senderRole === currentRole ? 'flex-row-reverse' : 'flex-row'">
                  <!-- Avatar small -->
                  <div class="shrink-0 mt-auto mb-1">
                    <div class="flex h-8 w-8 items-center justify-center rounded-full shadow-sm border border-app-border"
                         :class="msg.senderRole === currentRole ? 'bg-indigo-100' : 'bg-app-panel'">
                      <User v-if="msg.senderRole === 'PERSON'" class="w-4 h-4" :class="msg.senderRole === currentRole ? 'text-indigo-600' : 'text-app-subtext'" />
                      <Building2 v-else class="w-4 h-4" :class="msg.senderRole === currentRole ? 'text-indigo-600' : 'text-app-subtext'" />
                    </div>
                  </div>
                  
                  <!-- Bubble -->
                  <div class="group relative flex flex-col" :class="msg.senderRole === currentRole ? 'items-end' : 'items-start'">
                    <span class="text-[11px] text-app-subtext mb-1 ml-1 mr-1 font-medium">{{ msg.senderName }}</span>
                    <div
                      class="px-5 py-3 text-[15px] leading-relaxed shadow-sm relative"
                      :class="[
                        msg.senderRole === currentRole 
                          ? 'bg-indigo-600 text-white rounded-2xl rounded-br-sm border border-indigo-500' 
                          : 'bg-app-panel text-app-text rounded-2xl rounded-bl-sm border border-app-border/80'
                      ]"
                    >
                      <div class="whitespace-pre-wrap break-words">{{ msg.text }}</div>
                    </div>
                    <!-- Status -->
                    <div class="mt-1 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200" :class="msg.senderRole === currentRole ? 'mr-1' : 'ml-1'">
                      <Clock class="w-3 h-3 text-app-subtext" />
                      <span class="text-[10px] text-app-subtext">{{ fmt(msg.createdAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Input Area -->
          <div class="p-4 bg-app-bg/80 backdrop-blur-md border-t border-app-border/60 shrink-0">
            <div class="relative flex items-end gap-3 rounded-2xl bg-app-panel p-2 pr-3 shadow-sm border border-app-border ring-1 ring-slate-100/50 focus-within:ring-2 focus-within:ring-indigo-500 focus-within:border-indigo-500 transition-all duration-200">
              <el-input
                v-model="draft"
                type="textarea"
                :autosize="{ minRows: 1, maxRows: 5 }"
                resize="none"
                placeholder="输入消息内容，Enter 发送，Shift + Enter 换行..."
                class="chat-input !border-none !shadow-none bg-transparent"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <button
                type="button"
                class="shrink-0 flex h-9 w-9 items-center justify-center rounded-xl transition-all duration-200 shadow-sm disabled:cursor-not-allowed mb-1"
                :class="draft.trim() && !sending ? 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-md hover:-translate-y-0.5' : 'bg-app-bg text-app-subtext'"
                :disabled="!draft.trim() || sending"
                @click="sendMessage"
              >
                <Send class="w-4 h-4" :class="{'translate-x-[-1px] translate-y-[1px]': true}" />
              </button>
            </div>
            <div class="mt-2 text-center">
              <span class="text-[11px] text-app-subtext">使用 Enter 发送消息，Shift + Enter 换行</span>
            </div>
          </div>
        </template>
      </div>
      
    </div>
  </div>
</template>

<style scoped>
/* Custom Scrollbar for Chat */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 20px;
}
.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background-color: #94a3b8;
}

/* Override Element Plus input styles to make it seamless */
:deep(.chat-input .el-textarea__inner) {
  box-shadow: none !important;
  border: none !important;
  background-color: transparent !important;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.5;
  color: #1e293b;
}
:deep(.chat-input .el-textarea__inner:focus) {
  box-shadow: none !important;
}
:deep(.chat-input .el-textarea__inner::placeholder) {
  color: #94a3b8;
}
</style>
