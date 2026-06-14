<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import { LayoutDashboard, Users, FolderOpen, Database, Activity, LineChart, ClipboardList, LogOut, Sun, Moon } from 'lucide-vue-next'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { toggleTheme, isDark } = useTheme()

const menuItems = [
  { index: '/admin', label: '系统概览', icon: LayoutDashboard },
  { index: '/admin/users', label: '用户管理', icon: Users },
  { index: '/admin/docs', label: '文档库', icon: FolderOpen },
  { index: '/admin/data', label: '数据维护', icon: Database },
  { index: '/admin/match', label: '匹配记录', icon: Activity },
  { index: '/admin/monitor', label: '运营监控', icon: LineChart },
  { index: '/admin/audit', label: '审计日志', icon: ClipboardList },
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
  <div class="flex h-screen bg-app-bg">
    <!-- Sidebar -->
    <aside class="w-[260px] shrink-0 border-r border-app-border bg-app-panel flex flex-col">
      <!-- Brand -->
      <div class="flex h-16 items-center px-6 border-b border-app-border">
        <div class="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center mr-3">
          <Database class="w-5 h-5 text-white" />
        </div>
        <div>
          <div class="text-sm font-display font-bold text-app-text">管理后台</div>
          <div class="text-xs text-app-subtext">系统管理</div>
        </div>
      </div>

      <!-- Navigation -->
      <div class="flex-1 overflow-y-auto px-3 py-6">
        <div class="text-[11px] font-medium text-app-subtext mb-3 px-3 uppercase tracking-wider">管理菜单</div>
        <el-menu :default-active="active" class="border-0 !bg-transparent space-y-1 admin-menu" @select="onSelect">
          <el-menu-item v-for="i in menuItems" :key="i.index" :index="i.index" class="!h-11 !rounded-lg hover:!bg-app-bg transition-all !text-app-text !px-3 group">
            <div class="flex items-center w-full gap-3">
              <component :is="i.icon" class="w-5 h-5 text-app-subtext group-hover:text-emerald-600 transition-colors" :class="{'!text-emerald-600': active === i.index}" />
              <span class="text-sm font-medium flex-1">{{ i.label }}</span>
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
            {{ route.meta.title ?? '管理后台' }}
          </h1>
        </div>

        <div class="flex items-center shrink-0 space-x-3">
          <!-- Theme Toggle -->
          <button @click="toggleTheme" :aria-label="isDark ? '切换亮色模式' : '切换暗色模式'" class="p-2 text-app-subtext hover:text-emerald-600 transition-colors rounded-lg hover:bg-emerald-50" :title="isDark ? '切换亮色模式' : '切换暗色模式'">
            <Sun v-if="isDark" class="w-5 h-5" />
            <Moon v-else class="w-5 h-5" />
          </button>

          <!-- User Profile Dropdown -->
          <el-dropdown trigger="click" placement="bottom-end">
            <div class="flex items-center gap-2 cursor-pointer hover:bg-app-bg p-1.5 pr-3 rounded-lg transition-all">
              <div class="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 font-semibold text-sm">
                {{ auth.userId?.charAt(0).toUpperCase() || 'A' }}
              </div>
              <span class="text-sm font-medium text-app-text max-w-[80px] truncate">{{ auth.userId || '管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="w-48">
                <div class="px-3 py-2 mb-1 border-b border-app-border">
                  <div class="text-sm font-semibold text-app-text truncate">{{ auth.userId || '管理员' }}</div>
                  <div class="text-xs text-emerald-600 mt-0.5">系统管理员</div>
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
          <img src="/admin-bg.png" alt="" width="1920" height="1080" class="w-full h-full object-cover opacity-[0.35]" />
        </div>
        <div class="relative z-10 max-w-7xl mx-auto">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
:deep(.admin-menu .el-menu-item.is-active) {
  @apply bg-emerald-50 text-emerald-600 font-semibold;
  box-shadow: inset 3px 0 0 0 #10b981;
}
</style>
