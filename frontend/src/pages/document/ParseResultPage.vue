<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getParseResult, listCompanyTargets, publishResumeToTalentPool } from '@/api/document'
import { jobMarketMatchesForDocument } from '@/api/match'
import type { ParseResultVO } from '@/types/document'
import type { MatchListItem } from '@/types/match'
import { useDocumentStore } from '@/stores/document'
import { useMatchStore } from '@/stores/match'

const route = useRoute()
const router = useRouter()
const docsStore = useDocumentStore()
const matchStore = useMatchStore()

const docId = computed(() => String(route.params.docId || ''))
const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))

const loading = ref(true)
const data = ref<ParseResultVO | null>(null)
const jobMatches = ref<MatchListItem[]>([])
const jobMatchesLoading = ref(false)
const publishLoading = ref(false)
const companyTargets = ref<Array<{ account: string }>>([])
const selectedCompanyAccount = ref('')

const parsed = computed(() => {
  const v = data.value?.resultJson
  return v && typeof v === 'object' ? (v as any) : null
})

const skills = computed<string[]>(() => (Array.isArray(parsed.value?.skills) ? parsed.value.skills.filter((x: unknown) => typeof x === 'string') : []))
const educations = computed<Array<{ school?: string; degree?: string; major?: string }>>(() => (Array.isArray(parsed.value?.education) ? parsed.value.education : []))
const projects = computed<Array<{ name?: string; summary?: string }>>(() => (Array.isArray(parsed.value?.projects) ? parsed.value.projects : []))
const resumeCritique = computed(() => (typeof parsed.value?.resumeCritique === 'string' ? parsed.value.resumeCritique : ''))
const jobCritique = computed(() => (typeof parsed.value?.jobCritique === 'string' ? parsed.value.jobCritique : ''))
const canDeliver = computed(() => !!data.value?.canPublishToTalentPool)
const deliveredCompanyAccounts = computed(() => data.value?.deliveredCompanyAccounts ?? [])

const hollandRows = computed(() => {
  const raw = parsed.value?.hollandRiasec ?? parsed.value?.jobHollandRiasec
  if (!raw || typeof raw !== 'object') return []
  const names: Record<string, string> = { R: '现实型', I: '研究型', A: '艺术型', S: '社会型', E: '企业型', C: '常规型' }
  return (['R', 'I', 'A', 'S', 'E', 'C'] as const).map((key) => {
    const value = (raw[key] ?? raw[key.toLowerCase()]) as number | string | undefined
    const num = typeof value === 'number' ? value : Number(value)
    return { code: key, label: names[key], value: Number.isFinite(num) ? num : 0 }
  })
})

const refreshResult = async () => {
  data.value = await getParseResult(docId.value)
  docsStore.hydrate()
  docsStore.setResult(docId.value, data.value)
  docsStore.updateStatus(docId.value, data.value.status)
}

const loadJobMatches = async () => {
  if (isCompany.value || !docId.value) return
  jobMatchesLoading.value = true
  try {
    jobMatches.value = await jobMarketMatchesForDocument(docId.value, 0)
  } catch {
    jobMatches.value = []
  } finally {
    jobMatchesLoading.value = false
  }
}

const deliverResume = async () => {
  if (!selectedCompanyAccount.value) {
    ElMessage.warning('请先选择企业')
    return
  }
  publishLoading.value = true
  try {
    await publishResumeToTalentPool(docId.value, selectedCompanyAccount.value)
    await refreshResult()
    ElMessage.success(`已投递到 ${selectedCompanyAccount.value}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '投递失败')
  } finally {
    publishLoading.value = false
  }
}

const openJobMatchDetail = (row: MatchListItem) => {
  matchStore.hydrate()
  matchStore.addHistory(row, 'PERSON')
  router.push(`/person/match/detail/${encodeURIComponent(row.recordId)}`)
}

onMounted(async () => {
  matchStore.hydrate()
  loading.value = true
  try {
    if (!isCompany.value) {
      companyTargets.value = await listCompanyTargets()
      selectedCompanyAccount.value = companyTargets.value[0]?.account ?? ''
    }
    await refreshResult()
  } catch (e: any) {
    ElMessage.error(e?.message || '获取解析结果失败')
  } finally {
    loading.value = false
  }
})

watch(
  () => [data.value?.status, docId.value, isCompany.value] as const,
  ([status]) => {
    if (!isCompany.value && status === 'DONE') void loadJobMatches()
  },
  { immediate: true },
)

const goGraph = () => {
  router.push(`${base.value}/graph/${isCompany.value ? 'job-001' : 'person-001'}`)
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">解析结果</div>
          <div class="mt-1 text-sm text-app-text">DocId：{{ docId }}</div>
        </div>
        <div class="flex items-center gap-2">
          <el-button @click="router.push(`${base}/doc/task/${docId}`)">返回任务</el-button>
          <el-button type="primary" @click="goGraph">查看图谱</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div v-if="loading" class="text-sm text-app-text">加载中...</div>
      <div v-else-if="!data" class="text-sm text-app-text">暂无数据</div>
      <div v-else class="space-y-4">
        <el-card v-if="!isCompany && data.status === 'DONE'" shadow="never">
          <div class="flex flex-wrap items-center gap-3">
            <div class="flex-1 text-sm text-app-text">
              只有你主动选择企业投递后，简历才会进入该企业的人才库。每家企业只能看到投给自己的简历。
            </div>
            <el-select v-if="canDeliver" v-model="selectedCompanyAccount" placeholder="选择企业" style="width: 220px">
              <el-option v-for="company in companyTargets" :key="company.account" :label="company.account" :value="company.account" />
            </el-select>
            <el-button v-if="canDeliver" type="primary" :loading="publishLoading" @click="deliverResume">投递简历</el-button>
          </div>
          <div v-if="deliveredCompanyAccounts.length" class="mt-3 flex flex-wrap gap-2">
            <el-tag v-for="account in deliveredCompanyAccounts" :key="account" type="success">{{ account }}</el-tag>
          </div>
        </el-card>

        <el-card v-if="!isCompany && data.status === 'DONE'" shadow="never">
          <div class="text-sm font-semibold text-app-text">人才市场岗位匹配</div>
          <div class="mt-1 text-xs text-app-subtext">这里是简历对人才市场岗位的匹配，不依赖企业投递。</div>
          <div v-if="jobMatchesLoading" class="mt-3 text-sm text-app-text">加载中...</div>
          <el-table v-else class="mt-3" :data="jobMatches" size="small" max-height="360">
            <el-table-column prop="title" label="岗位" min-width="200" />
            <el-table-column prop="org" label="来源" min-width="160" />
            <el-table-column prop="score" label="匹配度" width="100">
              <template #default="{ row }">
                <el-tag type="success">{{ row.score }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openJobMatchDetail(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-tabs>
          <el-tab-pane v-if="resumeCritique || jobCritique || hollandRows.length" label="AI 分析">
            <el-card v-if="resumeCritique" shadow="never" class="mb-4">
              <div class="text-sm font-semibold text-app-text">简历分析</div>
              <pre class="mt-3 whitespace-pre-wrap rounded-lg bg-app-bg p-4 text-sm text-app-text">{{ resumeCritique }}</pre>
            </el-card>
            <el-card v-if="jobCritique" shadow="never" class="mb-4">
              <div class="text-sm font-semibold text-app-text">岗位分析</div>
              <pre class="mt-3 whitespace-pre-wrap rounded-lg bg-app-bg p-4 text-sm text-app-text">{{ jobCritique }}</pre>
            </el-card>
            <el-card v-if="hollandRows.length" shadow="never">
              <div class="text-sm font-semibold text-app-text">霍兰德 RIASEC</div>
              <el-table class="mt-3" :data="hollandRows" size="small" border>
                <el-table-column prop="code" label="维度" width="90" />
                <el-table-column prop="label" label="类型" width="120" />
                <el-table-column prop="value" label="得分" />
              </el-table>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="结构化结果">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="状态">{{ data.status }}</el-descriptions-item>
            </el-descriptions>

            <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
              <el-card shadow="never" class="lg:col-span-1">
                <div class="text-sm font-semibold text-app-text">技能</div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <el-tag v-for="s in skills" :key="s" type="success">{{ s }}</el-tag>
                  <div v-if="skills.length === 0" class="text-sm text-app-text">暂无</div>
                </div>
              </el-card>
              <el-card shadow="never" class="lg:col-span-2">
                <div class="text-sm font-semibold text-app-text">教育经历</div>
                <div class="mt-3 space-y-2">
                  <div v-for="(e, idx) in educations" :key="idx" class="rounded-lg border border-app-border bg-app-panel p-3">
                    <div class="text-sm text-app-text">{{ e.school || '未知学校' }}</div>
                    <div class="mt-1 text-xs text-app-subtext">{{ [e.degree, e.major].filter(Boolean).join(' / ') }}</div>
                  </div>
                  <div v-if="educations.length === 0" class="text-sm text-app-text">暂无</div>
                </div>
              </el-card>
            </div>

            <el-card shadow="never" class="mt-4">
              <div class="text-sm font-semibold text-app-text">项目经历</div>
              <div class="mt-3 space-y-2">
                <div v-for="(p, idx) in projects" :key="idx" class="rounded-lg border border-app-border bg-app-panel p-3">
                  <div class="text-sm text-app-text">{{ p.name || '未命名项目' }}</div>
                  <div class="mt-1 text-sm text-app-text">{{ p.summary || '暂无描述' }}</div>
                </div>
                <div v-if="projects.length === 0" class="text-sm text-app-text">暂无</div>
              </div>
            </el-card>

            <div class="mt-4">
              <el-collapse>
                <el-collapse-item title="解析结果 JSON" name="json">
                  <pre class="max-h-[420px] overflow-auto rounded-lg bg-zinc-950 p-4 text-xs text-zinc-100">{{ JSON.stringify(data.resultJson, null, 2) }}</pre>
                </el-collapse-item>
                <el-collapse-item title="证据" name="evidence">
                  <el-table :data="data.evidences || []" size="small">
                    <el-table-column prop="field" label="字段" width="160" />
                    <el-table-column prop="page" label="页码" width="90" />
                    <el-table-column prop="text" label="片段" />
                  </el-table>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>
