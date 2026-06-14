<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RadarChart from '@/components/RadarChart.vue'
import RiasecRadarChart from '@/components/RiasecRadarChart.vue'
import { getOriginalDocumentBlob, getParseResult } from '@/api/document'
import { getMatchDetail } from '@/api/match'
import type { ParseResultVO } from '@/types/document'
import type { MatchDetailVO } from '@/types/match'
import { useMatchStore } from '@/stores/match'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { useChatStore, type ChatThread } from '@/stores/chat'
import { 
  ArrowLeft, Bookmark, MessageSquare, Star, RefreshCw, AlertCircle,
  FileText, ExternalLink, Download, CheckCircle2, ChevronRight, User, Briefcase
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const matchStore = useMatchStore()
const auth = useAuthStore()
const audit = useAuditStore()
const chatStore = useChatStore()

const recordId = computed(() => String(route.params.recordId || ''))
const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))
const side = computed(() => (isCompany.value ? 'COMPANY' : 'PERSON'))
const userKey = computed(() => `${auth.userType ?? 'ANON'}:${auth.userId || 'anon'}`)

const loading = ref(true)
const detail = ref<MatchDetailVO | null>(null)
const candidateResume = ref<ParseResultVO | null>(null)
const candidateOriginalUrl = ref('')
const candidateOriginalLoading = ref(false)

const feedbackDlg = ref(false)
const feedbackForm = reactive<{ rating: 1 | 2 | 3 | 4 | 5; tags: string[]; comment: string }>({
  rating: 5,
  tags: [],
  comment: '',
})

const chatDrawerOpen = ref(false)
const chatDraft = ref('')
const chatScrollRef = ref<HTMLElement>()

const indicators = computed(() => {
  const obj = detail.value?.scoreBreakdown ?? {}
  return Object.keys(obj).map((k) => ({ name: k, max: 50 }))
})

const values = computed(() => {
  const obj = detail.value?.scoreBreakdown ?? {}
  return Object.keys(obj).map((k) => obj[k] ?? 0)
})

const riasec = computed(() => detail.value?.riasec ?? null)

const riasecPersonValues = computed(() => {
  const r = riasec.value?.person
  if (!r) return []
  return [r.r, r.i, r.a, r.s, r.e, r.c]
})

const riasecTargetValues = computed(() => {
  const r = riasec.value?.target
  if (!r) return []
  return [r.r, r.i, r.a, r.s, r.e, r.c]
})

const riasecCode = ['R', 'I', 'A', 'S', 'E', 'C']
const riasecName: Record<string, string> = {
  R: '现实型',
  I: '研究型',
  A: '艺术型',
  S: '社会型',
  E: '企业型',
  C: '常规型',
}

const topRiasec = (vals: number[]) => {
  const pairs = vals.map((v, idx) => ({ v, k: riasecCode[idx] }))
  pairs.sort((a, b) => b.v - a.v)
  return pairs.slice(0, 3).map((p) => `${p.k}${riasecName[p.k]}`).join(' / ')
}

const riasecPersonTop = computed(() => (riasecPersonValues.value.length ? topRiasec(riasecPersonValues.value) : '-'))
const riasecTargetTop = computed(() => (riasecTargetValues.value.length ? topRiasec(riasecTargetValues.value) : '-'))
const riasecTagType = computed(() => {
  if (!riasec.value) return 'info'
  if (riasec.value.similarity >= 75) return 'success'
  if (riasec.value.similarity >= 60) return 'warning'
  return 'danger'
})
const riasecTagText = computed(() => {
  if (!riasec.value) return '暂无'
  if (riasec.value.similarity >= 75) return '较匹配'
  if (riasec.value.similarity >= 60) return '一般'
  return '偏低'
})

const candidateResumeDocId = computed(() => (isCompany.value ? detail.value?.candidateDocumentId || '' : ''))

const candidateParsed = computed(() => {
  const v = candidateResume.value?.resultJson
  if (!v || typeof v !== 'object') return null
  return v as Record<string, unknown>
})

const candidateResumeSkills = computed<string[]>(() => {
  const s = candidateParsed.value?.skills
  return Array.isArray(s) ? s.filter((x): x is string => typeof x === 'string') : []
})

const candidateResumeCritique = computed(() => {
  const t = candidateParsed.value?.resumeCritique
  return typeof t === 'string' ? t : ''
})

const stripImprovementSection = (text: string) => {
  if (!text) return ''
  return text.replace(/\n*【改进建议】[\s\S]*$/u, '').trim()
}

const candidateResumeAnalysis = computed(() => stripImprovementSection(candidateResumeCritique.value).trim())

const candidateIsPdf = computed(() => {
  const fileName = candidateResume.value?.fileName?.toLowerCase() || ''
  return candidateResume.value?.fileType === 'PDF' || fileName.endsWith('.pdf')
})

const candidatePdfPreviewUrl = computed(() => {
  if (!candidateOriginalUrl.value) return ''
  return `${candidateOriginalUrl.value}#toolbar=0&navpanes=0&scrollbar=0&view=FitH`
})

const favorite = computed(() => matchStore.favoriteSet(userKey.value).has(recordId.value))
const feedbackList = computed(() => matchStore.feedbackByRecord(userKey.value, recordId.value))

const feedbackTags = [
  { label: '不相关', value: 'IRRELEVANT' },
  { label: '分数偏低', value: 'SCORE_LOW' },
  { label: '分数偏高', value: 'SCORE_HIGH' },
  { label: '技能识别错误', value: 'SKILL_WRONG' },
  { label: '建议不合理', value: 'SUGGESTION_BAD' },
]

const personAccount = computed(() => (isCompany.value ? detail.value?.candidateAccount?.trim() || '' : auth.userId.trim()))
const companyAccount = computed(() => (isCompany.value ? auth.userId.trim() : detail.value?.jobCompanyAccount?.trim() || ''))
const personName = computed(() => {
  if (isCompany.value) return detail.value?.candidateTitle?.trim() || personAccount.value || '个人用户'
  return auth.userId.trim() || '我'
})
const companyName = computed(() => {
  if (isCompany.value) return auth.userId.trim() || detail.value?.jobCompanyAccount?.trim() || '企业HR'
  return detail.value?.jobOrg?.trim() || detail.value?.jobCompanyAccount?.trim() || '企业HR'
})

const chatAvailable = computed(() => Boolean(personAccount.value && companyAccount.value))

const chatThread = ref<ChatThread | null>(null)
const chatThreadLoading = ref(false)

const syncChatThread = async () => {
  if (!chatAvailable.value) {
    chatThread.value = null
    return
  }
  chatThreadLoading.value = true
  try {
    chatThread.value = await chatStore.ensureThread({
      personAccount: personAccount.value,
      personName: personName.value,
      companyAccount: companyAccount.value,
      companyName: companyName.value,
      contextRecordId: recordId.value,
      contextTitle: isCompany.value ? detail.value?.candidateTitle || '候选人沟通' : detail.value?.jobTitle || '岗位沟通',
      contextOrg: isCompany.value ? detail.value?.candidateOrg || '' : detail.value?.jobOrg || '',
    })
  } catch (e: any) {
    chatThread.value = null
    ElMessage.warning(e?.message || '初始化会话失败')
  } finally {
    chatThreadLoading.value = false
  }
}

const activeThreadMessages = computed(() => (chatThread.value ? chatStore.messagesByThread(chatThread.value.id) : []))
const chatUnreadCount = computed(() => {
  if (!chatThread.value || !auth.userId.trim()) return 0
  return chatStore.unreadCount(chatThread.value.id, side.value, auth.userId.trim())
})

const chatCounterpartLabel = computed(() => {
  if (!chatAvailable.value) return '当前记录没有可聊天的对端身份'
  return isCompany.value ? `与 ${personName.value} 对话` : `与 ${companyName.value} 对话`
})

const chatContextText = computed(() => {
  if (!chatThread.value) return ''
  return `${chatThread.value.contextTitle}${chatThread.value.contextOrg ? ` · ${chatThread.value.contextOrg}` : ''}`
})

const chatLastMessageText = computed(() => {
  const last = activeThreadMessages.value[activeThreadMessages.value.length - 1]
  if (!last) return '还没有消息记录'
  return `${last.senderName}：${last.text}`
})

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

const revokeOriginalUrl = () => {
  if (candidateOriginalUrl.value) {
    URL.revokeObjectURL(candidateOriginalUrl.value)
    candidateOriginalUrl.value = ''
  }
}

const loadCandidateOriginal = async () => {
  revokeOriginalUrl()
  if (!candidateResumeDocId.value || candidateResume.value?.originalAvailable === false) return
  candidateOriginalLoading.value = true
  try {
    const blob = await getOriginalDocumentBlob(candidateResumeDocId.value)
    candidateOriginalUrl.value = URL.createObjectURL(blob)
  } catch (e: any) {
    candidateOriginalUrl.value = ''
    ElMessage.warning(e?.message || '加载原始文档失败')
  } finally {
    candidateOriginalLoading.value = false
  }
}

const openOriginal = () => {
  if (!candidateOriginalUrl.value) return
  window.open(candidateOriginalUrl.value, '_blank', 'noopener')
}

const downloadOriginal = () => {
  if (!candidateOriginalUrl.value) return
  const a = document.createElement('a')
  a.href = candidateOriginalUrl.value
  a.download = candidateResume.value?.fileName || `${candidateResumeDocId.value}.bin`
  a.click()
}

const scrollChatToBottom = async () => {
  await nextTick()
  const el = chatScrollRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const openChat = async () => {
  if (!chatAvailable.value) {
    ElMessage.warning('当前记录缺少可聊天的对端身份')
    return
  }
  if (!chatThread.value) await syncChatThread()
  if (!chatThread.value) return
  try {
    await chatStore.loadMessages(chatThread.value.id)
    await chatStore.markThreadRead(chatThread.value.id, side.value, auth.userId.trim())
  } catch (e: any) {
    ElMessage.error(e?.message || '加载消息失败')
    return
  }
  chatDrawerOpen.value = true
  audit.hydrate()
  audit.add({ module: 'chat.open', result: 'OK', detail: { threadId: chatThread.value.id, recordId: recordId.value, side: side.value } })
  await scrollChatToBottom()
}

const sendChatMessage = async () => {
  if (!chatThread.value || !auth.userId.trim()) return
  try {
    const sent = await chatStore.sendMessage({
      threadId: chatThread.value.id,
      senderRole: side.value,
      senderAccount: auth.userId.trim(),
      senderName: isCompany.value ? companyName.value : personName.value,
      text: chatDraft.value,
    })
    if (!sent) return
    chatDraft.value = ''
    await chatStore.markThreadRead(chatThread.value.id, side.value, auth.userId.trim())
    audit.hydrate()
    audit.add({ module: 'chat.send', result: 'OK', detail: { threadId: chatThread.value.id, recordId: recordId.value, side: side.value } })
    await scrollChatToBottom()
  } catch (e: any) {
    ElMessage.error(e?.message || '发送失败')
  }
}

const submitFeedback = async () => {
  if (!feedbackForm.comment.trim()) return ElMessage.warning('请填写反馈说明')
  matchStore.addFeedback({
    recordId: recordId.value,
    rating: feedbackForm.rating,
    tags: feedbackForm.tags,
    comment: feedbackForm.comment.trim(),
  })
  audit.hydrate()
  audit.add({
    module: 'match.feedback',
    result: 'OK',
    detail: { recordId: recordId.value, rating: feedbackForm.rating, tags: feedbackForm.tags },
  })
  feedbackDlg.value = false
  feedbackForm.rating = 5
  feedbackForm.tags = []
  feedbackForm.comment = ''
  ElMessage.success('反馈已记录')
}

const load = async () => {
  loading.value = true
  try {
    detail.value = await getMatchDetail(recordId.value)
    candidateResume.value = null
    revokeOriginalUrl()
    if (candidateResumeDocId.value) {
      try {
        candidateResume.value = await getParseResult(candidateResumeDocId.value)
        await loadCandidateOriginal()
      } catch {
        candidateResume.value = null
      }
    }
    matchStore.hydrate()
    await syncChatThread()
    if (detail.value) {
      const prev = matchStore.historyByUser(userKey.value).find((h) => h.recordId === recordId.value)
      matchStore.addHistory(
        {
          recordId: recordId.value,
          title: prev?.title ?? (isCompany.value ? detail.value.candidateTitle || `候选人 ${recordId.value}` : detail.value.jobTitle || `岗位 ${recordId.value}`),
          org: prev?.org ?? (isCompany.value ? detail.value.candidateOrg || '人才库推荐' : detail.value.jobOrg || '人才市场'),
          companyAccount: prev?.companyAccount ?? detail.value.jobCompanyAccount,
          score: detail.value.score,
        },
        side.value,
      )
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载详情失败')
  } finally {
    loading.value = false
  }
}

const toggleFav = () => {
  matchStore.toggleFavorite(recordId.value)
  audit.hydrate()
  audit.add({ module: 'match.favorite.toggle', result: 'OK', detail: { recordId: recordId.value } })
}

const clearFeedback = async () => {
  await ElMessageBox.confirm('仅清空当前记录的本地反馈，确认继续？', '提示', { type: 'warning' })
  matchStore.hydrate()
  matchStore.feedbacks = matchStore.feedbacks.filter((f) => !(f.userKey === userKey.value && f.recordId === recordId.value))
  matchStore.persist()
  audit.hydrate()
  audit.add({ module: 'match.feedback.clear', result: 'OK', detail: { recordId: recordId.value } })
}

const getScoreProgressColor = (score: number) => {
  if (score >= 90) return '#10b981'
  if (score >= 75) return '#3b82f6'
  if (score >= 60) return '#f59e0b'
  return '#94a3b8'
}

watch(chatDrawerOpen, async (open) => {
  if (!open || !chatThread.value || !auth.userId.trim()) return
  try {
    await chatStore.markThreadRead(chatThread.value.id, side.value, auth.userId.trim())
  } catch {
    /* ignore */
  }
  await scrollChatToBottom()
})

watch(
  () => activeThreadMessages.value.length,
  async () => {
    if (!chatDrawerOpen.value) return
    await scrollChatToBottom()
  },
)

onMounted(() => {
  matchStore.hydrate()
  void chatStore.hydrate()
  load()
})

onBeforeUnmount(revokeOriginalUrl)
</script>

<template>
  <div class="space-y-6 max-w-[1400px] mx-auto">
    <!-- Action Header -->
    <div class="bg-app-panel dark:bg-slate-900 rounded-2xl p-5 border border-app-border dark:border-slate-800 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
      <div class="flex items-center gap-4">
        <el-button @click="router.push(`${base}/match/${isCompany ? 'candidates' : 'jobs'}`)" class="!border-none !bg-app-bg dark:!bg-slate-800 hover:!bg-slate-200 dark:hover:!bg-slate-700 !text-app-text dark:!text-slate-300">
          <ArrowLeft class="w-4 h-4 mr-1" /> 返回列表
        </el-button>
        <div>
          <div class="text-lg font-bold text-app-text dark:text-white flex items-center gap-2">
            匹配详情报告
            <el-tag size="small" type="info" class="font-mono">{{ recordId }}</el-tag>
          </div>
        </div>
      </div>
      
      <div class="flex items-center gap-2 w-full sm:w-auto">
        <el-button :type="favorite ? 'warning' : 'info'" plain class="flex-1 sm:flex-none" @click="toggleFav">
          <Bookmark class="w-4 h-4 mr-1" :class="{'fill-current': favorite}" />
          {{ favorite ? '已收藏' : '收藏' }}
        </el-button>
        <el-button v-if="chatAvailable" type="success" plain class="flex-1 sm:flex-none" @click="openChat">
          <MessageSquare class="w-4 h-4 mr-1" />
          沟通
          <span v-if="chatUnreadCount" class="ml-1 bg-red-500 text-white rounded-full px-1.5 py-0.5 text-[10px] leading-none">{{ chatUnreadCount }}</span>
        </el-button>
        <el-button type="primary" plain class="flex-1 sm:flex-none" @click="feedbackDlg = true">
          <Star class="w-4 h-4 mr-1" /> 评价
        </el-button>
        <el-button :loading="loading" @click="load" class="!px-3" title="刷新">
          <RefreshCw class="w-4 h-4" :class="{'animate-spin': loading}" />
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-app-subtext">
      <RefreshCw class="w-8 h-8 animate-spin mb-4 text-indigo-500" />
      <span>正在加载匹配报告及雷达图...</span>
    </div>
    
    <div v-else-if="!detail" class="flex flex-col items-center justify-center py-20 text-app-subtext bg-app-panel dark:bg-slate-900 rounded-2xl border border-app-border dark:border-slate-800">
      <AlertCircle class="w-12 h-12 mb-4 text-app-subtext" />
      <span>未找到相关的匹配数据</span>
    </div>

    <div v-else class="grid grid-cols-1 gap-6 lg:grid-cols-12">
      <!-- Left Column: Core Stats -->
      <div class="space-y-6 lg:col-span-4 xl:col-span-3">
        <!-- Score Card -->
        <div class="rounded-2xl border border-app-border dark:border-slate-800 bg-app-panel dark:bg-slate-900 p-6 shadow-sm flex flex-col items-center justify-center relative overflow-hidden">
          <div class="absolute inset-0 bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/10 dark:to-purple-900/10"></div>
          <div class="relative z-10 text-center w-full">
            <h3 class="text-sm font-semibold text-app-subtext dark:text-app-subtext mb-4 uppercase tracking-wider">综合匹配度</h3>
            <el-progress type="dashboard" :percentage="detail.score" :width="160" :stroke-width="12" :color="getScoreProgressColor(detail.score)">
              <template #default="{ percentage }">
                <div class="flex flex-col items-center">
                  <span class="text-4xl font-black" :style="{ color: getScoreProgressColor(detail.score) }">{{ percentage }}</span>
                  <span class="text-xs font-medium text-app-subtext mt-1">/ 100</span>
                </div>
              </template>
            </el-progress>
          </div>
        </div>

        <!-- Chat Shortcut -->
        <div v-if="chatAvailable" class="rounded-2xl border border-emerald-200 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-900/20 p-5 cursor-pointer hover:bg-emerald-100 dark:hover:bg-emerald-900/30 transition-colors group" @click="openChat">
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <MessageSquare class="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                <span class="text-sm font-bold text-emerald-900 dark:text-emerald-300 truncate">沟通通道</span>
              </div>
              <div class="text-xs text-emerald-700 dark:text-emerald-500/80 truncate font-medium">{{ chatCounterpartLabel }}</div>
              <div class="mt-2 text-xs text-emerald-600/80 dark:text-emerald-400/70 line-clamp-2 leading-relaxed bg-app-panel/50 dark:bg-black/20 p-2 rounded-lg border border-emerald-100 dark:border-emerald-800/50">{{ chatLastMessageText }}</div>
            </div>
            <div class="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-800 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
              <ChevronRight class="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            </div>
          </div>
        </div>

        <!-- Missing Skills -->
        <div class="rounded-2xl border border-rose-200 dark:border-rose-900/30 bg-app-panel dark:bg-slate-900 p-5">
          <h3 class="text-sm font-bold text-app-text dark:text-slate-200 mb-4 flex items-center gap-2">
            <AlertCircle class="w-4 h-4 text-rose-500" />
            能力差距项
          </h3>
          <div class="space-y-3">
            <div v-for="s in detail.missingSkills" :key="s.name" class="flex flex-col gap-1 p-2.5 rounded-xl bg-rose-50 dark:bg-rose-900/10 border border-rose-100 dark:border-rose-900/30">
              <div class="flex items-center justify-between">
                <span class="text-sm font-semibold text-rose-900 dark:text-rose-300">{{ s.name }}</span>
                <span class="text-xs font-bold text-rose-600 dark:text-rose-400 bg-rose-100 dark:bg-rose-900/50 px-2 py-0.5 rounded-full">差距 {{ s.gap }}</span>
              </div>
            </div>
            <div v-if="detail.missingSkills.length === 0" class="text-sm text-app-subtext flex items-center justify-center py-4 bg-app-bg dark:bg-slate-800/50 rounded-xl">
              <CheckCircle2 class="w-4 h-4 mr-2 text-emerald-500" /> 完全覆盖，无明显短板
            </div>
          </div>
        </div>

        <!-- Rationales -->
        <div class="rounded-2xl border border-app-border dark:border-slate-800 bg-app-panel dark:bg-slate-900 p-5">
          <h3 class="text-sm font-bold text-app-text dark:text-slate-200 mb-4 flex items-center gap-2">
            <FileText class="w-4 h-4 text-blue-500" />
            系统解析摘要
          </h3>
          <ul class="space-y-3">
            <li v-for="(r, idx) in detail.rationales || []" :key="idx" class="text-sm text-app-text dark:text-slate-300 leading-relaxed flex items-start gap-2 bg-app-bg dark:bg-slate-800/50 p-3 rounded-xl">
              <span class="w-1.5 h-1.5 rounded-full bg-blue-500 mt-2 shrink-0"></span>
              {{ r }}
            </li>
            <li v-if="(detail.rationales || []).length === 0" class="text-sm text-app-subtext text-center py-4">暂无分析摘要</li>
          </ul>
        </div>
      </div>

      <!-- Right Column: Charts & Details -->
      <div class="space-y-6 lg:col-span-8 xl:col-span-9">
        
        <!-- Radar Charts Row -->
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
          <!-- Skills Breakdown -->
          <div class="rounded-3xl border border-app-border/60 dark:border-zinc-800/60 bg-app-panel/70 dark:bg-zinc-900/70 backdrop-blur-xl p-6 shadow-sm h-[400px] flex flex-col hover:shadow-md transition-shadow">
            <h3 class="text-base font-bold text-app-text dark:text-zinc-100 mb-2">匹配度拆解雷达</h3>
            <div class="flex-1 relative -mt-2">
              <RadarChart :indicators="indicators" :values="values" />
            </div>
          </div>

          <!-- RIASEC -->
          <div v-if="riasec" class="rounded-3xl border border-app-border/60 dark:border-zinc-800/60 bg-app-panel/70 dark:bg-zinc-900/70 backdrop-blur-xl p-6 shadow-sm h-[400px] flex flex-col hover:shadow-md transition-shadow">
            <div class="flex items-start justify-between mb-4 gap-2">
              <div class="min-w-0">
                <h3 class="text-base font-bold text-app-text dark:text-zinc-100 truncate">霍兰德职业兴趣 (RIASEC)</h3>
                <div class="mt-1.5 text-xs text-app-subtext dark:text-zinc-400 font-medium leading-relaxed truncate" :title="`主导特征：${riasecPersonTop} (人) vs ${riasecTargetTop} (岗)`">
                  <span class="text-indigo-600 dark:text-indigo-400 font-bold">人</span> {{ riasecPersonTop }}
                  <span class="mx-1.5 text-slate-300 dark:text-zinc-600">|</span>
                  <span class="text-emerald-600 dark:text-emerald-400 font-bold">岗</span> {{ riasecTargetTop }}
                </div>
              </div>
              <el-tag :type="riasecTagType" effect="dark" round class="font-bold shrink-0 shadow-sm">{{ riasec.similarity }}% {{ riasecTagText }}</el-tag>
            </div>
            <div class="flex-1 relative -mt-2">
              <RiasecRadarChart
                :person-values="riasecPersonValues"
                :target-values="riasecTargetValues"
                :labels="{ person: isCompany ? '人才' : '个人', target: '岗位' }"
              />
            </div>
          </div>
        </div>

        <!-- Matched Skills & Suggestions -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div v-if="!isCompany" class="rounded-2xl border border-emerald-200 dark:border-emerald-900/30 bg-app-panel dark:bg-slate-900 p-5">
            <h3 class="text-sm font-bold text-app-text dark:text-slate-200 mb-4 flex items-center gap-2">
              <CheckCircle2 class="w-4 h-4 text-emerald-500" />
              已匹配技能
            </h3>
            <div class="flex flex-wrap gap-2">
              <el-tag
                v-for="s in detail.matchedSkills"
                :key="s.name"
                type="success"
                class="!bg-emerald-50 !text-emerald-700 !border-emerald-200 dark:!bg-emerald-900/30 dark:!text-emerald-300 dark:!border-emerald-800 py-1 h-auto leading-normal px-3"
                round
              >
                {{ s.name }}
              </el-tag>
              <div v-if="detail.matchedSkills.length === 0" class="text-sm text-app-subtext w-full text-center py-4 bg-app-bg dark:bg-slate-800/50 rounded-xl">无直接匹配项</div>
            </div>
          </div>

          <div class="rounded-2xl border border-amber-200 dark:border-amber-900/30 bg-app-panel dark:bg-slate-900 p-5" :class="{'md:col-span-2': isCompany}">
            <h3 class="text-sm font-bold text-app-text dark:text-slate-200 mb-4 flex items-center gap-2">
              <Star class="w-4 h-4 text-amber-500" />
              发展与提升建议
            </h3>
            <ul class="space-y-3">
              <li v-for="(s, idx) in detail.suggestions || []" :key="idx" class="text-sm text-app-text dark:text-slate-300 leading-relaxed bg-amber-50/50 dark:bg-amber-900/10 p-3 rounded-xl border border-amber-100 dark:border-amber-900/20">
                {{ s }}
              </li>
              <li v-if="(detail.suggestions || []).length === 0" class="text-sm text-app-subtext text-center py-4">系统认为当前状态较好，暂无补充建议。</li>
            </ul>
          </div>
        </div>

        <!-- Original Resume Section (Company Only) -->
        <div v-if="isCompany && candidateResume" class="rounded-2xl border border-app-border dark:border-slate-800 bg-app-panel dark:bg-slate-900 p-5">
          <div class="flex flex-col sm:flex-row items-start justify-between gap-4 mb-4">
            <div>
              <h3 class="text-sm font-bold text-app-text dark:text-slate-200 flex items-center gap-2">
                <User class="w-4 h-4 text-indigo-500" />候选人原始简历
              </h3>
              <div class="mt-1 text-xs text-app-subtext font-mono">
                {{ candidateResume.fileName || candidateResumeDocId }}
              </div>
            </div>
            <div class="flex items-center gap-2 w-full sm:w-auto">
              <el-button v-if="candidateOriginalUrl" type="primary" plain class="flex-1 sm:flex-none" @click="openOriginal">
                <ExternalLink class="w-4 h-4 mr-1" /> 独立窗口打开
              </el-button>
              <el-button v-if="candidateOriginalUrl" type="info" plain class="flex-1 sm:flex-none" @click="downloadOriginal">
                <Download class="w-4 h-4" />
              </el-button>
            </div>
          </div>

          <el-alert
            v-if="candidateResume.originalAvailable === false"
            type="warning"
            :closable="false"
            show-icon
            class="!rounded-xl"
            title="此份历史简历未保存原始文件格式，建议提醒求职者重新上传。"
          />

          <div v-else-if="candidateOriginalLoading" class="py-12 flex justify-center text-app-subtext">
            <RefreshCw class="w-6 h-6 animate-spin text-indigo-500 mr-2" /> 正在加载原始文档...
          </div>

          <div v-else-if="candidateOriginalUrl && candidateIsPdf" class="overflow-hidden rounded-xl border border-app-border dark:border-slate-700 bg-app-bg dark:bg-slate-800">
            <object :data="candidatePdfPreviewUrl" type="application/pdf" class="h-[500px] w-full">
              <div class="flex h-[500px] items-center justify-center text-sm text-app-subtext">
                当前浏览器环境不支持内嵌 PDF 预览，请点击右上角按钮查看。
              </div>
            </object>
          </div>

          <div v-else-if="candidateOriginalUrl" class="rounded-xl border border-app-border dark:border-slate-700 bg-app-bg dark:bg-slate-800/50 p-8 text-center">
            <FileText class="w-12 h-12 text-app-subtext mx-auto mb-3" />
            <div class="text-sm text-app-text dark:text-slate-300 font-medium">当前简历是 Word 格式</div>
            <div class="text-xs text-app-subtext mt-1">出于兼容性考虑，请下载或在新窗口中打开查看。</div>
          </div>
        </div>

        <!-- Evidences Table -->
        <div class="rounded-2xl border border-app-border dark:border-slate-800 bg-app-panel dark:bg-slate-900 p-5">
          <h3 class="text-sm font-bold text-app-text dark:text-slate-200 mb-4 flex items-center gap-2">
            <Database class="w-4 h-4 text-app-subtext" />
            分析证据追溯
          </h3>
          <div class="rounded-xl overflow-hidden border border-app-border dark:border-slate-700">
            <el-table :data="detail.evidences || []" style="width: 100%" :header-cell-style="{ background: 'var(--el-fill-color-light)' }">
              <el-table-column prop="type" label="能力类型" width="120">
                <template #default="{ row }">
                  <el-tag size="small" type="info" class="!bg-app-bg dark:!bg-slate-800 !border-none text-app-text dark:text-slate-300 font-bold">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="field" label="考察字段" width="140" />
              <el-table-column prop="weight" label="模型权重" width="90">
                <template #default="{ row }">
                  <span class="font-mono text-xs text-indigo-600 dark:text-indigo-400">{{ row.weight }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="snippet" label="原文提取片段" min-width="300" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="text-app-text dark:text-app-subtext">{{ row.snippet }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

      </div>
    </div>
  </div>

  <!-- Dialogs & Drawers ... (Keep existing feedback & chat UI but use updated styles) -->
  <el-dialog v-model="feedbackDlg" title="提交人工评价反馈" width="500px" class="!rounded-2xl">
    <el-form label-position="top">
      <el-form-item label="本次匹配质量打分">
        <el-rate v-model="feedbackForm.rating" size="large" />
      </el-form-item>
      <el-form-item label="存在的问题 (可选)">
        <el-checkbox-group v-model="feedbackForm.tags" class="flex flex-col gap-2">
          <el-checkbox v-for="t in feedbackTags" :key="t.value" :label="t.value" :value="t.value" border class="!mr-0">{{ t.label }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="详细说明">
        <el-input
          v-model="feedbackForm.comment"
          type="textarea"
          :rows="4"
          placeholder="请说明您认为不准确或不合理的原因，帮助模型迭代优化..."
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="feedbackDlg = false">取消</el-button>
      <el-button type="primary" @click="submitFeedback" class="!bg-indigo-600 hover:!bg-indigo-700 !border-none">提交评价</el-button>
    </template>
  </el-dialog>

  <!-- Chat Drawer (Stylized) -->
  <el-drawer v-model="chatDrawerOpen" size="460px" :with-header="false" destroy-on-close class="!bg-app-bg dark:!bg-slate-950">
    <div class="flex h-full flex-col">
      <div class="bg-app-panel dark:bg-slate-900 border-b border-app-border dark:border-slate-800 px-6 py-5 shadow-sm z-10">
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="text-lg font-bold text-app-text dark:text-white flex items-center gap-2">
              <MessageSquare class="w-5 h-5 text-emerald-500" />
              在线沟通
            </div>
            <div class="mt-2 text-sm font-medium text-emerald-600 dark:text-emerald-400">{{ chatCounterpartLabel }}</div>
            <div class="mt-1 text-xs text-app-subtext truncate max-w-[300px]">{{ chatContextText }}</div>
          </div>
          <el-button circle @click="chatDrawerOpen = false" class="!border-none !bg-app-bg hover:!bg-slate-200 dark:!bg-slate-800">✕</el-button>
        </div>
      </div>

      <div ref="chatScrollRef" class="flex-1 overflow-y-auto px-5 py-6">
        <div v-if="activeThreadMessages.length === 0" class="flex flex-col h-full items-center justify-center text-app-subtext">
          <MessageSquare class="w-12 h-12 mb-3 opacity-20" />
          <span class="text-sm">暂无聊天记录，主动打个招呼吧。</span>
        </div>
        <div v-else class="space-y-6">
          <div
            v-for="msg in activeThreadMessages"
            :key="msg.id"
            class="flex"
            :class="msg.senderRole === side ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[85%] relative group"
            >
              <div class="text-[11px] font-medium text-app-subtext mb-1" :class="msg.senderRole === side ? 'text-right' : 'text-left'">
                {{ msg.senderName }} · {{ fmt(msg.createdAt).split(' ')[1] }}
              </div>
              <div
                class="px-4 py-3 text-sm leading-relaxed shadow-sm break-words"
                :class="msg.senderRole === side 
                  ? 'rounded-2xl rounded-tr-sm bg-emerald-500 text-white' 
                  : 'rounded-2xl rounded-tl-sm bg-app-panel dark:bg-slate-800 text-app-text dark:text-slate-200 border border-app-border dark:border-slate-700'"
              >
                <div class="whitespace-pre-wrap">{{ msg.text }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-app-panel dark:bg-slate-900 border-t border-app-border dark:border-slate-800 p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
        <el-input
          v-model="chatDraft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入消息，按 Enter 发送..."
          class="!text-sm"
          @keydown.enter.exact.prevent="sendChatMessage"
        />
        <div class="mt-3 flex items-center justify-between">
          <div class="text-[11px] text-app-subtext px-2">Enter 发送 / Shift+Enter 换行</div>
          <el-button
            type="success"
            :disabled="!chatDraft.trim() || !chatThread"
            @click="sendChatMessage"
            class="!px-6 !rounded-lg shadow-sm"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>
