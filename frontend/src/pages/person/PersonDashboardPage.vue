<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { animate, stagger } from 'animejs'
import { useAuthStore } from '@/stores/auth'
import { useDocumentStore } from '@/stores/document'
import { useMatchStore } from '@/stores/match'
import { useChatStore } from '@/stores/chat'
import { FileText, Network, Briefcase, MessageSquare, ArrowRight, Star, History, CheckCircle } from 'lucide-vue-next'

const router = useRouter()
const auth = useAuthStore()
const docStore = useDocumentStore()
const matchStore = useMatchStore()
const chatStore = useChatStore()

const bannerRef = ref<HTMLDivElement>()
const statsRef = ref<HTMLDivElement>()
const cardsRef = ref<HTMLDivElement>()

onMounted(() => {
  docStore.hydrate()
  matchStore.hydrate()

  // 尊重用户的减少动画偏好设置
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  try {
    if (!prefersReducedMotion) {
      animate(bannerRef.value!, {
        opacity: [0, 1],
        translateY: [20, 0],
        duration: 500,
      })
      animate(`${statsRef.value} > div`, {
        translateY: [20, 0],
        opacity: [0, 1],
        duration: 400,
        delay: stagger(80),
      })
      animate(`${cardsRef.value} > div`, {
        translateY: [24, 0],
        opacity: [0, 1],
        duration: 450,
        delay: stagger(100),
      })
    }
  } catch {
    // 静默降级
  }
})

const userKey = computed(() => `${auth.userType ?? 'ANON'}:${auth.userId || 'anon'}`)

const docCount = computed(() => docStore.docs.length)
const docDoneCount = computed(() => docStore.docs.filter((d) => d.status === 'DONE').length)
const historyCount = computed(() => matchStore.historyByUser(userKey.value).length)
const favoriteCount = computed(() => (matchStore.favorites[userKey.value] ?? []).length)
const threadCount = computed(() => chatStore.threads.length)
const unreadCount = computed(() => {
  const account = auth.userId.trim()
  if (!account) return 0
  return chatStore.threads.reduce((total, thread) => total + chatStore.unreadCount(thread.id, 'PERSON', account), 0)
})

const statsCards = computed(() => [
  { label: '已上传文档', value: docCount.value, sub: `${docDoneCount.value} 份已解析`, icon: FileText, color: 'indigo' },
  { label: '浏览记录', value: historyCount.value, sub: '人才市场浏览', icon: History, color: 'purple' },
  { label: '收藏岗位', value: favoriteCount.value, sub: '已收藏职位', icon: Star, color: 'amber' },
  { label: '消息会话', value: threadCount.value, sub: unreadCount.value > 0 ? `${unreadCount.value} 条未读` : '暂无未读', icon: MessageSquare, color: 'rose' },
])

const colorMap: Record<string, { bg: string; text: string; iconBg: string }> = {
  indigo: { bg: 'bg-indigo-50', text: 'text-indigo-600', iconBg: 'bg-indigo-100' },
  purple: { bg: 'bg-purple-50', text: 'text-purple-600', iconBg: 'bg-purple-100' },
  amber: { bg: 'bg-amber-50', text: 'text-amber-600', iconBg: 'bg-amber-100' },
  rose: { bg: 'bg-rose-50', text: 'text-rose-600', iconBg: 'bg-rose-100' },
}
</script>

<template>
  <div class="space-y-8 pb-8 w-full">
    <!-- Welcome Banner -->
    <div ref="bannerRef" class="relative overflow-hidden rounded-2xl bg-gradient-to-r from-indigo-600 to-indigo-700 p-10 text-white shadow-lg">
      <div class="absolute right-0 top-0 -mt-10 -mr-10 w-64 h-64 rounded-full bg-app-panel/10 blur-3xl pointer-events-none"></div>
      <div class="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div class="max-w-xl">
          <h2 class="text-3xl font-display font-bold mb-3">
            欢迎回来，{{ auth.userId || '用户' }}
          </h2>
          <p class="text-indigo-100 text-base leading-relaxed">
            上传您的简历文档进行 AI 解析，通过能力图谱算法发现与您技能匹配的职业机会。
          </p>
        </div>
        
        <router-link to="/person/doc/list" class="shrink-0 bg-app-panel text-indigo-600 font-semibold px-6 py-3 rounded-lg hover:bg-indigo-50 transition-all duration-200 shadow-sm flex items-center gap-2 no-underline">
          <FileText class="w-5 h-5" />
          文档中心
          <ArrowRight class="w-4 h-4" />
        </router-link>
      </div>
    </div>

    <!-- Stats Row -->
    <div ref="statsRef" class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="s in statsCards" :key="s.label" class="bg-app-panel rounded-xl border border-app-border p-5 flex items-center gap-4 hover:shadow-sm transition-shadow">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0" :class="[colorMap[s.color]?.iconBg, colorMap[s.color]?.text]">
          <component :is="s.icon" class="w-6 h-6" />
        </div>
        <div class="min-w-0">
          <div class="text-2xl font-bold text-app-text">{{ s.value }}</div>
          <div class="text-xs text-app-subtext truncate">{{ s.label }}</div>
          <div class="text-xs text-app-subtext truncate">{{ s.sub }}</div>
        </div>
      </div>
    </div>

    <!-- Quick Links Grid -->
    <div>
      <h3 class="text-lg font-semibold text-app-text mb-5">快捷入口</h3>
      
      <div ref="cardsRef" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5">

        <!-- Card 1: 能力图谱 -->
        <router-link to="/person/graph/person-001" class="bg-app-panel rounded-xl border border-app-border p-6 group hover:shadow-md hover:border-indigo-200 transition-all duration-300 flex flex-col no-underline">
          <div class="w-12 h-12 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center mb-4 group-hover:bg-indigo-100 transition-colors">
            <Network class="w-6 h-6" />
          </div>
          <h4 class="text-base font-semibold text-app-text mb-2">能力图谱</h4>
          <p class="text-sm text-app-subtext leading-relaxed flex-1">可视化展示您的技能关联与能力结构，AI 驱动的能力分析。</p>
          <div class="mt-4 flex items-center text-sm font-medium text-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity">
            查看 <ArrowRight class="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
          </div>
        </router-link>

        <!-- Card 2: 人才市场 -->
        <router-link to="/person/match/jobs" class="bg-app-panel rounded-xl border border-app-border p-6 group hover:shadow-md hover:border-purple-200 transition-all duration-300 flex flex-col no-underline">
          <div class="w-12 h-12 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center mb-4 group-hover:bg-purple-100 transition-colors">
            <Briefcase class="w-6 h-6" />
          </div>
          <h4 class="text-base font-semibold text-app-text mb-2">人才市场</h4>
          <p class="text-sm text-app-subtext leading-relaxed flex-1">浏览为您精选的职业机会，基于霍兰德模型与技能的智能匹配推荐。</p>
          <div class="mt-4 flex items-center text-sm font-medium text-purple-600 opacity-0 group-hover:opacity-100 transition-opacity">
            探索 <ArrowRight class="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
          </div>
        </router-link>

        <!-- Card 3: 消息中心 -->
        <router-link to="/person/messages" class="bg-app-panel rounded-xl border border-app-border p-6 group hover:shadow-md hover:border-rose-200 transition-all duration-300 flex flex-col no-underline">
          <div class="w-12 h-12 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center mb-4 group-hover:bg-rose-100 transition-colors">
            <MessageSquare class="w-6 h-6" />
          </div>
          <h4 class="text-base font-semibold text-app-text mb-2">消息中心</h4>
          <p class="text-sm text-app-subtext leading-relaxed flex-1">与企业沟通互动，管理面试邀请与在线沟通记录。</p>
          <div class="mt-4 flex items-center text-sm font-medium text-rose-600 opacity-0 group-hover:opacity-100 transition-opacity">
            查看 <ArrowRight class="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
          </div>
        </router-link>

        <!-- Card 4: 文档中心 -->
        <router-link to="/person/doc/list" class="bg-app-panel rounded-xl border border-app-border p-6 group hover:shadow-md hover:border-teal-200 transition-all duration-300 flex flex-col no-underline">
          <div class="w-12 h-12 rounded-lg bg-teal-50 text-teal-600 flex items-center justify-center mb-4 group-hover:bg-teal-100 transition-colors">
            <FileText class="w-6 h-6" />
          </div>
          <h4 class="text-base font-semibold text-app-text mb-2">文档中心</h4>
          <p class="text-sm text-app-subtext leading-relaxed flex-1">管理个人简历及各类文档，查看 AI 提取的结构化信息。</p>
          <div class="mt-4 flex items-center text-sm font-medium text-teal-600 opacity-0 group-hover:opacity-100 transition-opacity">
            访问 <ArrowRight class="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
          </div>
        </router-link>
        
      </div>
    </div>
  </div>
</template>
