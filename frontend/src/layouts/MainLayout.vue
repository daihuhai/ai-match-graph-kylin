<script setup lang="ts">
import { computed, onMounted, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import { useChatStore, type ChatRole } from '@/stores/chat'
import { LayoutDashboard, FolderOpen, Share2, Briefcase, MessageSquare, Search, LogOut, Sun, Moon } from 'lucide-vue-next'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { toggleTheme, isDark } = useTheme()
const chatStore = useChatStore()

onMounted(() => {
  if (auth.isLoggedIn && auth.userType !== 'ADMIN') {
    void chatStore.hydrate()
  }
})

const basePath = computed(() => (auth.userType === 'COMPANY' ? '/company' : '/person'))
const currentRole = computed(() => (auth.userType === 'COMPANY' ? 'COMPANY' : 'PERSON') as ChatRole)
// 使用实际用户 ID 作为图谱默认 subjectId，避免硬编码
const defaultSubjectId = computed(() => auth.userId || 'default')

const myThreads = computed(() => chatStore.threads)

const currentThreadCount = computed(() => myThreads.value.length)

const currentUnreadCount = computed(() => {
  const account = auth.userId.trim()
  if (!account) return 0
  return myThreads.value.reduce((total, thread) => total + chatStore.unreadCount(thread.id, currentRole.value, account), 0)
})

const menuItems = computed(() => {
  if (auth.userType === 'COMPANY') {
    return [
      { index: `${basePath.value}/dashboard`, label: '工作台', icon: LayoutDashboard },
      { index: `${basePath.value}/doc/list`, label: '文档中心', icon: FolderOpen },
      { index: `${basePath.value}/graph/${defaultSubjectId.value}`, label: '职位图谱', icon: Share2 },
      { index: `${basePath.value}/match/candidates`, label: '人才库', icon: Search },
      { index: `${basePath.value}/messages`, label: `消息${currentThreadCount.value ? ` (${currentThreadCount.value})` : ''}`, icon: MessageSquare },
    ]
  }
  return [
    { index: `${basePath.value}/dashboard`, label: '工作台', icon: LayoutDashboard },
    { index: `${basePath.value}/doc/list`, label: '文档中心', icon: FolderOpen },
    { index: `${basePath.value}/graph/${defaultSubjectId.value}`, label: '能力图谱', icon: Share2 },
    { index: `${basePath.value}/match/jobs`, label: '人才市场', icon: Briefcase },
    { index: `${basePath.value}/messages`, label: `消息${currentThreadCount.value ? ` (${currentThreadCount.value})` : ''}`, icon: MessageSquare },
  ]
})

const active = computed(() => {
  const p = route.path
  if (p.startsWith(`${basePath.value}/doc/`)) return `${basePath.value}/doc/list`
  if (p.startsWith(`${basePath.value}/graph/`)) {
    return `${basePath.value}/graph/${defaultSubjectId.value}`
  }
  if (p.startsWith(`${basePath.value}/match/`)) {
    return auth.userType === 'COMPANY' ? `${basePath.value}/match/candidates` : `${basePath.value}/match/jobs`
  }
  if (p.startsWith(`${basePath.value}/messages`)) return `${basePath.value}/messages`
  const hit = menuItems.value.find((i) => p === i.index || p.startsWith(`${i.index}/`))
  return hit?.index ?? menuItems.value[0]?.index ?? ''
})

const onSelect = (index: string) => {
  router.push(index)
}

const openMessages = () => {
  router.push(`${basePath.value}/messages`)
}

const logout = () => {
  auth.logout()
  router.replace({ name: 'Login' })
}

watchEffect(() => {
  const unreadPrefix = currentUnreadCount.value > 0 ? `(${currentUnreadCount.value}) ` : ''
  const pageTitle = typeof route.meta.title === 'string' && route.meta.title ? route.meta.title : 'AI智能匹配与能力图谱系统'
  document.title = `${unreadPrefix}${pageTitle}`
})
</script>

<template>
  <div class="flex h-screen bg-app-bg">
    <!-- Sidebar -->
    <aside class="w-[260px] shrink-0 border-r border-app-border bg-app-panel flex flex-col">
      <div class="flex h-16 items-center px-6 border-b border-app-border">
        <div class="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center mr-3">
          <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <div>
          <div class="text-sm font-display font-bold text-app-text">AI 智能匹配</div>
          <div class="text-xs text-app-subtext">
            {{ auth.userType === 'COMPANY' ? '企业中心' : '个人中心' }}
          </div>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto px-3 py-6">
        <div class="text-[11px] font-medium text-app-subtext mb-3 px-3 uppercase tracking-wider">导航菜单</div>
        <el-menu :default-active="active" class="border-0 !bg-transparent space-y-1" @select="onSelect">
          <el-menu-item v-for="i in menuItems" :key="i.index" :index="i.index" class="!h-11 !rounded-lg hover:!bg-app-bg transition-all !text-app-text !px-3 group">
            <div class="flex items-center w-full gap-3">
              <component :is="i.icon" class="w-5 h-5 text-app-subtext group-hover:text-indigo-600 transition-colors" :class="{'!text-indigo-600': active === i.index}" />
              <el-badge
                v-if="i.index === `${basePath}/messages` && currentUnreadCount > 0"
                :value="currentUnreadCount > 99 ? '99+' : currentUnreadCount"
                :max="99"
                class="flex-1 text-left"
              >
                <span class="text-sm font-medium">{{ i.label }}</span>
              </el-badge>
              <span v-else class="text-sm font-medium flex-1">{{ i.label }}</span>
            </div>
          </el-menu-item>
        </el-menu>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- Header -->
      <header class="shrink-0 h-16 flex items-center justify-between px-8 bg-app-panel border-b border-app-border shadow-sm">
        <div class="flex items-center min-w-0">
          <h1 class="text-base font-semibold text-app-text truncate">
            {{ route.meta.title ?? '系统' }}
          </h1>
        </div>

        <div class="flex items-center shrink-0 space-x-3">
          <!-- Theme Toggle -->
          <button @click="toggleTheme" :aria-label="isDark ? '切换亮色模式' : '切换暗色模式'" class="p-2 text-app-subtext hover:text-indigo-600 transition-colors rounded-lg hover:bg-indigo-50" :title="isDark ? '切换亮色模式' : '切换暗色模式'">
            <Sun v-if="isDark" class="w-5 h-5" />
            <Moon v-else class="w-5 h-5" />
          </button>

          <button @click="openMessages" :aria-label="`消息中心${currentUnreadCount > 0 ? `（${currentUnreadCount > 99 ? '99+' : currentUnreadCount} 条未读）` : ''}`" class="relative p-2 text-app-subtext hover:text-indigo-600 transition-colors rounded-lg hover:bg-indigo-50">
            <el-badge v-if="currentUnreadCount > 0" :value="currentUnreadCount > 99 ? '99+' : currentUnreadCount" :max="99" class="absolute -top-1 -right-1 z-10" />
            <MessageSquare class="w-5 h-5" />
          </button>

          <div class="w-px h-6 border-app-border"></div>

          <!-- User Profile Dropdown -->
          <el-dropdown trigger="click" placement="bottom-end">
            <div class="flex items-center gap-2 cursor-pointer hover:bg-app-bg p-1.5 pr-3 rounded-lg transition-all">
              <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600 font-semibold text-sm">
                {{ auth.userId?.charAt(0).toUpperCase() || 'U' }}
              </div>
              <span class="text-sm font-medium text-app-text max-w-[80px] truncate">{{ auth.userId || '用户' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="w-48">
                <div class="px-3 py-2 mb-1 border-b border-app-border">
                  <div class="text-sm font-semibold text-app-text truncate">{{ auth.userId || '用户' }}</div>
                  <div class="text-xs text-indigo-600 mt-0.5">{{ auth.userType === 'COMPANY' ? '企业端' : '个人端' }}</div>
                </div>
                <el-dropdown-item @click="logout" class="!text-red-600 hover:!bg-red-50">
                  <LogOut class="w-4 h-4 mr-2" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Scrollable Main -->
      <main class="flex-1 overflow-y-auto px-8 py-6 relative">
        <!-- Background Image -->
        <div class="absolute inset-0 z-0 pointer-events-none">
          <img src="/client-bg.png" alt="" width="1920" height="1080" class="w-full h-full object-cover opacity-[0.35]" />
        </div>
        <div class="relative z-10 max-w-7xl mx-auto">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-menu-item.is-active) {
  @apply bg-indigo-50 text-indigo-600 font-semibold;
  box-shadow: inset 3px 0 0 0 #4f46e5;
}
</style>
