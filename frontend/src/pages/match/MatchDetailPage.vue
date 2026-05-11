<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RadarChart from '@/components/RadarChart.vue'
import RiasecRadarChart from '@/components/RiasecRadarChart.vue'
import { getMatchDetail } from '@/api/match'
import type { MatchDetailVO } from '@/types/match'
import { useMatchStore } from '@/stores/match'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'

const route = useRoute()
const router = useRouter()
const matchStore = useMatchStore()
const auth = useAuthStore()
const audit = useAuditStore()

const recordId = computed(() => String(route.params.recordId || ''))
const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))
const side = computed(() => (isCompany.value ? 'COMPANY' : 'PERSON'))
const userKey = computed(() => `${auth.userType ?? 'ANON'}:${auth.userId || 'anon'}`)

const loading = ref(true)
const detail = ref<MatchDetailVO | null>(null)

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

const topRiasec = (values: number[]) => {
  const pairs = values.map((v, idx) => ({ v, k: riasecCode[idx] }))
  pairs.sort((a, b) => b.v - a.v)
  return pairs.slice(0, 3).map((p) => `${p.k}${riasecName[p.k]}`).join(' / ')
}

const riasecPersonTop = computed(() => (riasecPersonValues.value.length ? topRiasec(riasecPersonValues.value) : '—'))
const riasecTargetTop = computed(() => (riasecTargetValues.value.length ? topRiasec(riasecTargetValues.value) : '—'))

const favorite = computed(() => matchStore.favoriteSet(userKey.value).has(recordId.value))
const feedbackList = computed(() => matchStore.feedbackByRecord(userKey.value, recordId.value))

const feedbackDlg = ref(false)
const feedbackForm = reactive<{ rating: 1 | 2 | 3 | 4 | 5; tags: string[]; comment: string }>({
  rating: 5,
  tags: [],
  comment: '',
})

const feedbackTags = [
  { label: '不相关', value: 'IRRELEVANT' },
  { label: '分数偏低', value: 'SCORE_LOW' },
  { label: '分数偏高', value: 'SCORE_HIGH' },
  { label: '技能识别错误', value: 'SKILL_WRONG' },
  { label: '建议不合理', value: 'SUGGESTION_BAD' },
]

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
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
  ElMessage.success('反馈已记录（本地）')
}

const load = async () => {
  loading.value = true
  try {
    detail.value = await getMatchDetail(recordId.value)
    matchStore.hydrate()
    if (detail.value) {
      const prev = matchStore.historyByUser(userKey.value).find((h) => h.recordId === recordId.value)
      matchStore.addHistory(
        {
          recordId: recordId.value,
          title: prev?.title ?? `匹配记录 ${recordId.value}`,
          org: prev?.org ?? (isCompany.value ? '候选人推荐' : '职位推荐'),
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

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div class="text-base font-semibold">匹配详情</div>
          <div class="mt-1 text-sm text-zinc-600">RecordId：{{ recordId }}</div>
        </div>
        <div class="flex items-center gap-2">
          <el-button @click="router.push(`${base}/match/${isCompany ? 'candidates' : 'jobs'}`)">返回列表</el-button>
          <el-button :type="favorite ? 'warning' : 'info'" @click="toggleFav">{{ favorite ? '已收藏' : '收藏' }}</el-button>
          <el-button type="primary" @click="feedbackDlg = true">反馈</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div v-if="loading" class="text-sm text-zinc-600">加载中...</div>
      <div v-else-if="!detail" class="text-sm text-zinc-600">暂无数据</div>
      <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div class="lg:col-span-1 space-y-4">
          <div class="rounded-xl border border-zinc-200 bg-white p-4">
            <div class="text-sm font-semibold text-zinc-700">总分</div>
            <div class="mt-2 text-3xl font-semibold text-zinc-900">{{ detail.score }}</div>
            <div class="mt-2">
              <el-progress :percentage="detail.score" :stroke-width="10" />
            </div>
          </div>

          <div class="rounded-xl border border-zinc-200 bg-white p-4">
            <div class="text-sm font-semibold text-zinc-700">解释要点</div>
            <ul class="mt-3 space-y-1 text-sm text-zinc-700">
              <li v-for="(r, idx) in detail.rationales || []" :key="idx">{{ r }}</li>
              <li v-if="(detail.rationales || []).length === 0" class="text-zinc-600">暂无</li>
            </ul>
          </div>

          <div class="rounded-xl border border-zinc-200 bg-white p-4">
            <div class="text-sm font-semibold text-zinc-700">缺失技能</div>
            <div class="mt-3 space-y-2">
              <div v-for="s in detail.missingSkills" :key="s.name" class="flex items-center justify-between">
                <div class="text-sm text-zinc-800">{{ s.name }}</div>
                <el-tag type="danger">差距 {{ s.gap }}</el-tag>
              </div>
              <div v-if="detail.missingSkills.length === 0" class="text-sm text-zinc-600">暂无</div>
            </div>
          </div>
        </div>

        <div class="lg:col-span-2 space-y-4">
          <RadarChart title="分项拆解（示例）" :indicators="indicators" :values="values" />

          <el-card v-if="riasec" shadow="never">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div class="text-sm font-semibold text-zinc-700">霍兰德 RIASEC（六维画像）</div>
                <div class="mt-1 text-xs text-zinc-500">
                  个人主导：{{ riasecPersonTop }}；目标主导：{{ riasecTargetTop }}；相似度：{{ riasec.similarity }}%
                </div>
              </div>
              <el-tag :type="riasec.similarity >= 75 ? 'success' : riasec.similarity >= 60 ? 'warning' : 'danger'">
                {{ riasec.similarity >= 75 ? '较匹配' : riasec.similarity >= 60 ? '一般' : '偏低' }}
              </el-tag>
            </div>
            <div class="mt-3">
              <RiasecRadarChart
                title="RIASEC 雷达图"
                :person-values="riasecPersonValues"
                :target-values="riasecTargetValues"
                :labels="{ person: isCompany ? '候选人' : '你', target: isCompany ? '岗位' : '岗位' }"
              />
            </div>
          </el-card>

          <el-card shadow="never">
            <div class="text-sm font-semibold text-zinc-700">技能覆盖</div>
            <div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
              <div class="rounded-lg border border-zinc-200 bg-white p-3">
                <div class="text-xs text-zinc-500">已满足</div>
                <div class="mt-2 flex flex-wrap gap-2">
                  <el-tag v-for="s in detail.matchedSkills" :key="s.name" type="success">
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
            <div class="text-sm font-semibold text-zinc-700">证据（示例）</div>
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
        <el-input v-model="feedbackForm.comment" type="textarea" :rows="4" placeholder="说明你认为不准确/不合理的原因，便于后续优化。" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="feedbackDlg = false">取消</el-button>
      <el-button type="primary" @click="submitFeedback">提交</el-button>
    </template>
  </el-dialog>
</template>
