<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { toggleTheme, isDark } = useTheme()

const menuItems = [
  { index: '/admin', label: '概览' },
  { index: '/admin/users', label: '用户管理' },
  { index: '/admin/docs', label: '文档库' },
  { index: '/admin/data', label: '数据维护' },
  { index: '/admin/match', label: '匹配记录' },
  { index: '/admin/monitor', label: '运营监控' },
  { index: '/admin/audit', label: '日志审计' },
]

const active = computed(() => {
  const p = route.path
  const hit = menuItems.find((i) => p === i.index || p.startsWith(`${i.index}/`))
  return hit?.index ?? '/admin'
})

const onSelect = (index: string) => {
  router.push(index)
}

const logout = () => {
  auth.logout()
  router.replace({ name: 'Login' })
}
</script>

<template>
  <el-container class="min-h-screen bg-zinc-50 dark:bg-zinc-950">
    <el-aside width="240px" class="border-r border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
      <div class="px-4 py-4">
        <div class="text-sm font-semibold text-zinc-900 dark:text-zinc-100">平台管理</div>
        <div class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">管理员控制台</div>
      </div>
      <el-menu :default-active="active" class="border-0" @select="onSelect">
        <el-menu-item v-for="i in menuItems" :key="i.index" :index="i.index">
          <span>{{ i.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="flex items-center justify-between border-b border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
        <div class="text-sm text-zinc-700 dark:text-zinc-200">{{ route.meta.title ?? '管理端' }}</div>
        <div class="flex items-center gap-2">
          <el-button size="small" @click="toggleTheme">{{ isDark ? '浅色' : '暗色' }}</el-button>
          <el-tag size="small" type="warning">{{ auth.userId || 'admin' }}</el-tag>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="p-6">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
