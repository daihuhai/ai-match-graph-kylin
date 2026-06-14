import { createRouter, createWebHistory } from 'vue-router'
import { routes } from '@/router/routes'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

let hydrated = false

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!hydrated) {
    auth.hydrate()
    hydrated = true
  }

  // 标题由各布局组件自行管理（MainLayout 带未读数前缀，AuthLayout 使用固定标题）
  document.title = 'AI智能匹配与能力图谱系统'

  if (to.meta?.public) return true
  if (!auth.isLoggedIn) return { name: 'Login', query: { redirect: to.fullPath } }

  const allowed = to.meta?.userTypes
  if (allowed && auth.userType && !allowed.includes(auth.userType)) return { name: 'Forbidden' }
  return true
})

export default router
