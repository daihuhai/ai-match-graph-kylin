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
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div class="text-base font-semibold">匹配详情</div>
          <div class="mt-1 text-sm text-zinc-600">RecordId：{{ recordId }}</div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <el-button @click="router.push(`${base}/match/${isCompany ? 'candidates' : 'jobs'}`)">返回列表</el-button>
          <el-button :type="favorite ? 'warning' : 'info'" @click="toggleFav">{{ favorite ? '已收藏' : '收藏' }}</el-button>
          <el-button v-if="chatAvailable" type="success" plain @click="openChat">
            聊一聊
            <span v-if="chatUnreadCount" class="ml-1 text-xs">({{ chatUnreadCount }})</span>
          </el-button>
          <el-button type="primary" @click="feedbackDlg = true">反馈</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div v-if="loading" class="text-sm text-zinc-600">加载中...</div>
      <div v-else-if="!detail" class="text-sm text-zinc-600">暂无数据</div>
      <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div class="space-y-4 lg:col-span-1">
          <div class="rounded-lg border border-zinc-200 bg-white p-4">
            <div class="text-sm font-semibold text-zinc-700">综合得分</div>
            <div class="mt-2 text-3xl font-semibold text-zinc-900">{{ detail.score }}</div>
            <div class="mt-2">
              <el-progress :percentage="detail.score" :stroke-width="10" />
            </div>
          </div>

          <div class="rounded-lg border border-zinc-200 bg-white p-4">
            <div class="text-sm font-semibold text-zinc-700">解释要点</div>
            <ul class="mt-3 space-y-1 text-sm text-zinc-700">
              <li v-for="(r, idx) in detail.rationales || []" :key="idx">{{ r }}</li>
              <li v-if="(detail.rationales || []).length === 0" class="text-zinc-600">暂无</li>
            </ul>
          </div>

          <div class="rounded-lg border border-zinc-200 bg-white p-4">
            <div class="text-sm font-semibold text-zinc-700">待补技能</div>
            <div class="mt-3 space-y-2">
              <div v-for="s in detail.missingSkills" :key="s.name" class="flex items-center justify-between gap-3">
                <div class="text-sm text-zinc-800">{{ s.name }}</div>
                <el-tag type="danger">差距 {{ s.gap }}</el-tag>
              </div>
              <div v-if="detail.missingSkills.length === 0" class="text-sm text-zinc-600">暂无</div>
            </div>
          </div>

          <div v-if="chatAvailable" class="rounded-lg border border-emerald-200 bg-emerald-50 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-emerald-900">聊天窗口</div>
                <div class="mt-1 text-sm text-emerald-700">{{ chatCounterpartLabel }}</div>
                <div class="mt-2 text-xs text-emerald-700">{{ chatLastMessageText }}</div>
              </div>
              <el-button type="success" plain @click="openChat">聊一聊</el-button>
            </div>
          </div>
        </div>

        <div class="space-y-4 lg:col-span-2">
          <RadarChart title="分项拆解" :indicators="indicators" :values="values" />

          <el-card v-if="riasec" shadow="never">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-zinc-700">RIASEC 霍兰德画像</div>
                <div class="mt-1 text-xs text-zinc-500">
                  人才主导：{{ riasecPersonTop }}；目标主导：{{ riasecTargetTop }}；相似度：{{ riasec.similarity }}%
                </div>
              </div>
              <el-tag :type="riasecTagType">{{ riasecTagText }}</el-tag>
            </div>
            <div class="mt-3">
              <RiasecRadarChart
                title="RIASEC 雷达图"
                :person-values="riasecPersonValues"
                :target-values="riasecTargetValues"
                :labels="{ person: isCompany ? '人才' : '我', target: '岗位' }"
              />
            </div>
          </el-card>

          <el-card v-if="isCompany && candidateResume" shadow="never">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-zinc-700">候选人原始简历</div>
                <div class="mt-1 text-xs text-zinc-500">
                  {{ candidateResume.fileName || candidateResumeDocId }}
                  <span v-if="candidateResume.fileType"> · {{ candidateResume.fileType }}</span>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <el-tag type="info">个人上传</el-tag>
                <el-button v-if="candidateOriginalUrl" link type="primary" @click="openOriginal">打开原件</el-button>
                <el-button v-if="candidateOriginalUrl" link @click="downloadOriginal">下载原件</el-button>
              </div>
            </div>

            <el-alert
              v-if="candidateResume.originalAvailable === false"
              class="mt-3"
              type="warning"
              :closable="false"
              title="这份历史简历没有保存原始文件，请让个人端重新上传后再查看原件。"
            />

            <div v-else-if="candidateOriginalLoading" class="mt-4 text-sm text-zinc-600">正在加载原件...</div>

            <div v-else-if="candidateOriginalUrl && candidateIsPdf" class="mt-4 overflow-hidden rounded-lg border border-zinc-200 bg-zinc-50">
              <object :data="candidatePdfPreviewUrl" type="application/pdf" class="h-[360px] w-full bg-white md:h-[420px]">
                <div class="flex h-[360px] items-center justify-center bg-white text-sm text-zinc-600 md:h-[420px]">
                  当前环境不支持内嵌 PDF 预览，请使用“打开原件”查看。
                </div>
              </object>
            </div>

            <div v-else-if="candidateOriginalUrl" class="mt-4 rounded-lg border border-zinc-200 bg-zinc-50 p-4">
              <div class="text-sm text-zinc-700">当前原件是 Word 文档，请使用“打开原件”或“下载原件”查看。</div>
            </div>

            <div v-else class="mt-4 rounded-lg border border-zinc-200 bg-zinc-50 p-4 text-sm text-zinc-700">
              暂时无法加载这份原件。
            </div>
          </el-card>

          <el-card v-if="isCompany && candidateResume" shadow="never">
            <div class="text-sm font-semibold text-zinc-700">简历分析</div>

            <div v-if="candidateResumeSkills.length" class="mt-3 flex flex-wrap gap-2">
              <el-tag
                v-for="skill in candidateResumeSkills"
                :key="skill"
                type="success"
                class="max-w-full whitespace-normal break-all py-1 leading-6"
              >
                {{ skill }}
              </el-tag>
            </div>

            <pre
              v-if="candidateResumeAnalysis"
              class="mt-4 whitespace-pre-wrap rounded-lg bg-zinc-50 p-3 text-sm leading-6 text-zinc-800"
            >{{ candidateResumeAnalysis }}</pre>

            <div v-else class="mt-3 text-sm text-zinc-600">暂无简历分析。</div>
          </el-card>

          <el-card shadow="never">
            <div class="text-sm font-semibold text-zinc-700">技能覆盖</div>
            <div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
              <div v-if="!isCompany" class="rounded-lg border border-zinc-200 bg-white p-3">
                <div class="text-xs text-zinc-500">已满足</div>
                <div class="mt-2 flex flex-wrap gap-2">
                  <el-tag
                    v-for="s in detail.matchedSkills"
                    :key="s.name"
                    type="success"
                    class="max-w-full whitespace-normal break-all py-1 leading-6"
                  >
                    {{ s.name }}
                  </el-tag>
                  <div v-if="detail.matchedSkills.length === 0" class="text-sm text-zinc-600">暂无</div>
                </div>
              </div>
              <div class="rounded-lg border border-zinc-200 bg-white p-3">
                <div class="text-xs text-zinc-500">建议</div>
                <ul class="mt-2 space-y-1 text-sm text-zinc-700">
                  <li v-for="(s, idx) in detail.suggestions || []" :key="idx">{{ s }}</li>
                  <li v-if="(detail.suggestions || []).length === 0" class="text-zinc-600">暂无</li>
                </ul>
              </div>
            </div>
          </el-card>

          <el-card shadow="never">
            <div class="text-sm font-semibold text-zinc-700">证据</div>
            <div class="mt-3">
              <el-table :data="detail.evidences || []" size="small">
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="field" label="字段" width="140" />
                <el-table-column prop="weight" label="权重" width="90" />
                <el-table-column prop="snippet" label="内容" />
              </el-table>
            </div>
          </el-card>

          <el-card shadow="never">
            <div class="flex items-center justify-between gap-2">
              <div class="text-sm font-semibold text-zinc-700">你的反馈</div>
              <el-button v-if="feedbackList.length" link type="danger" @click="clearFeedback">清空</el-button>
            </div>
            <div class="mt-3 space-y-2">
              <div v-for="f in feedbackList" :key="f.id" class="rounded-lg border border-zinc-200 bg-white p-3">
                <div class="flex items-center justify-between">
                  <el-rate :model-value="f.rating" disabled />
                  <div class="text-xs text-zinc-500">{{ fmt(f.createdAt) }}</div>
                </div>
                <div class="mt-2 flex flex-wrap gap-2">
                  <el-tag v-for="t in f.tags" :key="t" type="info">{{ t }}</el-tag>
                </div>
                <div class="mt-2 text-sm text-zinc-700">{{ f.comment }}</div>
              </div>
              <div v-if="feedbackList.length === 0" class="text-sm text-zinc-600">暂无</div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>

  <el-dialog v-model="feedbackDlg" title="提交反馈" width="560px">
    <el-form label-position="top">
      <el-form-item label="满意度">
        <el-rate v-model="feedbackForm.rating" />
      </el-form-item>
      <el-form-item label="标签">
        <el-checkbox-group v-model="feedbackForm.tags">
          <el-checkbox v-for="t in feedbackTags" :key="t.value" :label="t.value">{{ t.label }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          v-model="feedbackForm.comment"
          type="textarea"
          :rows="4"
          placeholder="说明你认为不准确或不合理的原因，便于后续优化。"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="feedbackDlg = false">取消</el-button>
      <el-button type="primary" @click="submitFeedback">提交</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="chatDrawerOpen" size="460px" :with-header="false" destroy-on-close>
    <div class="flex h-full flex-col">
      <div class="border-b border-zinc-200 px-5 py-4">
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="text-base font-semibold text-zinc-900">聊一聊</div>
            <div class="mt-1 text-sm text-zinc-600">{{ chatCounterpartLabel }}</div>
            <div class="mt-1 text-xs text-zinc-500">{{ chatContextText }}</div>
          </div>
          <el-button text @click="chatDrawerOpen = false">关闭</el-button>
        </div>
      </div>

      <div ref="chatScrollRef" class="flex-1 overflow-y-auto bg-zinc-50 px-4 py-4">
        <div v-if="activeThreadMessages.length === 0" class="flex h-full items-center justify-center text-sm text-zinc-500">
          先打个招呼吧。
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="msg in activeThreadMessages"
            :key="msg.id"
            class="flex"
            :class="msg.senderRole === side ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[82%] rounded-lg border border-zinc-200 px-3 py-2 text-sm leading-6 shadow-sm"
              :class="msg.senderRole === side ? 'border-emerald-500 bg-emerald-500 text-white' : 'bg-white text-zinc-800'"
            >
              <div class="text-xs opacity-80">{{ msg.senderName }}</div>
              <div class="mt-1 whitespace-pre-wrap break-words">{{ msg.text }}</div>
              <div class="mt-1 text-[11px] opacity-70">{{ fmt(msg.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="border-t border-zinc-200 bg-white px-4 py-4">
        <el-input
          v-model="chatDraft"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="输入想和对方聊的内容"
          @keydown.enter.exact.prevent="sendChatMessage"
        />
        <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
          <div class="text-xs text-zinc-500">Enter 发送</div>
          <button
            type="button"
            class="inline-flex h-10 w-28 items-center justify-center rounded-md bg-emerald-500 px-4 text-sm font-medium text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:bg-emerald-300"
            :disabled="!chatDraft.trim() || !chatThread"
            @click="sendChatMessage"
          >
            发送
          </button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>
