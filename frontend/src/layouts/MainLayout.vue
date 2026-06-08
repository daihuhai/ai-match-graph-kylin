<script setup lang="ts">
import { computed, onMounted, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import { useChatStore, type ChatRole } from '@/stores/chat'

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
      { index: `${basePath.value}/dashboard`, label: '工作台' },
      { index: `${basePath.value}/doc/list`, label: '文档中心' },
      { index: `${basePath.value}/graph/job-001`, label: '职位图谱' },
      { index: `${basePath.value}/match/candidates`, label: '人才库推荐' },
      { index: `${basePath.value}/messages`, label: `消息中心${currentThreadCount.value ? ` (${currentThreadCount.value})` : ''}` },
    ]
  }
  return [
    { index: `${basePath.value}/dashboard`, label: '工作台' },
    { index: `${basePath.value}/doc/list`, label: '文档中心' },
    { index: `${basePath.value}/graph/person-001`, label: '能力图谱' },
    { index: `${basePath.value}/match/jobs`, label: '人才市场' },
    { index: `${basePath.value}/messages`, label: `消息中心${currentThreadCount.value ? ` (${currentThreadCount.value})` : ''}` },
  ]
})

const active = computed(() => {
  const p = route.path
  if (p.startsWith(`${basePath.value}/doc/`)) return `${basePath.value}/doc/list`
  if (p.startsWith(`${basePath.value}/graph/`)) {
    return auth.userType === 'COMPANY' ? `${basePath.value}/graph/job-001` : `${basePath.value}/graph/person-001`
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
  <el-container class="min-h-screen bg-zinc-50 dark:bg-zinc-950">
    <el-aside width="220px" class="border-r border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
      <div class="px-4 py-4">
        <div class="text-sm font-semibold text-zinc-900 dark:text-zinc-100">AI智能匹配与能力图谱系统</div>
        <div class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
          {{ auth.userType === 'COMPANY' ? '企业端' : '个人端' }}
        </div>
      </div>
      <el-menu :default-active="active" class="border-0" @select="onSelect">
        <el-menu-item v-for="i in menuItems" :key="i.index" :index="i.index">
          <el-badge
            v-if="i.index === `${basePath}/messages` && currentUnreadCount > 0"
            :value="currentUnreadCount > 99 ? '99+' : currentUnreadCount"
            :max="99"
          >
            <span>{{ i.label }}</span>
          </el-badge>
          <span v-else>{{ i.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="flex items-center justify-between border-b border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
        <div class="text-sm text-zinc-700 dark:text-zinc-200">{{ route.meta.title ?? '页面' }}</div>
        <div class="flex items-center gap-2">
          <el-badge v-if="currentUnreadCount > 0" :value="currentUnreadCount > 99 ? '99+' : currentUnreadCount" :max="99">
            <el-button size="small" @click="openMessages">消息</el-button>
          </el-badge>
          <el-button v-else size="small" @click="openMessages">消息</el-button>
          <el-button size="small" @click="toggleTheme">{{ isDark ? '浅色' : '深色' }}</el-button>
          <el-tag size="small" type="info">{{ auth.userId || 'demo-user' }}</el-tag>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="p-6">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
