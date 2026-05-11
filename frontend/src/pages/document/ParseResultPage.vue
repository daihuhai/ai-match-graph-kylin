<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getParseResult } from '@/api/document'
import type { ParseResultVO } from '@/types/document'
import { useDocumentStore } from '@/stores/document'

const route = useRoute()
const router = useRouter()
const docsStore = useDocumentStore()

const docId = computed(() => String(route.params.docId || ''))
const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))

const loading = ref(true)
const data = ref<ParseResultVO | null>(null)

const parsed = computed(() => {
  const v = data.value?.resultJson
  if (!v || typeof v !== 'object') return null
  return v as any
})

const skills = computed<string[]>(() => {
  const s = parsed.value?.skills
  return Array.isArray(s) ? s.filter((x: any) => typeof x === 'string') : []
})

const educations = computed<Array<{ school?: string; degree?: string; major?: string }>>(() => {
  const e = parsed.value?.education
  return Array.isArray(e) ? e : []
})

const projects = computed<Array<{ name?: string; summary?: string }>>(() => {
  const p = parsed.value?.projects
  return Array.isArray(p) ? p : []
})

const resumeCritique = computed(() => {
  const t = parsed.value?.resumeCritique
  return typeof t === 'string' ? t : ''
})

const jobCritique = computed(() => {
  const t = parsed.value?.jobCritique
  return typeof t === 'string' ? t : ''
})

const hollandRiasec = computed(() => {
  const h = parsed.value?.hollandRiasec ?? parsed.value?.jobHollandRiasec
  if (!h || typeof h !== 'object') return null
  return h as Record<string, number>
})

const hollandRows = computed(() => {
  const h = hollandRiasec.value
  if (!h) return []
  const keys = ['R', 'I', 'A', 'S', 'E', 'C'] as const
  const name: Record<string, string> = {
    R: '现实型',
    I: '研究型',
    A: '艺术型',
    S: '社会型',
    E: '企业型',
    C: '常规型',
  }
  return keys.map((k) => {
    const low = k.toLowerCase()
    const raw = (h as Record<string, unknown>)[k] ?? (h as Record<string, unknown>)[low]
    const v = typeof raw === 'number' ? raw : Number(raw)
    return { code: k, label: name[k] ?? k, value: Number.isFinite(v) ? v : 0 }
  })
})

onMounted(async () => {
  loading.value = true
  try {
    data.value = await getParseResult(docId.value)
    if (data.value) {
      docsStore.hydrate()
      docsStore.setResult(docId.value, data.value)
      docsStore.updateStatus(docId.value, data.value.status)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取解析结果失败')
  } finally {
    loading.value = false
  }
})

const goGraph = () => {
  const subjectId = isCompany.value ? 'job-001' : 'person-001'
  router.push(`${base.value}/graph/${subjectId}`)
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">解析结果</div>
          <div class="mt-1 text-sm text-zinc-600">DocId：{{ docId }}</div>
        </div>
        <div class="flex items-center gap-2">
          <el-button @click="router.push(`${base}/doc/task/${docId}`)">返回任务</el-button>
          <el-button type="primary" @click="goGraph">查看图谱</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div v-if="loading" class="text-sm text-zinc-600">加载中...</div>
      <div v-else-if="!data" class="text-sm text-zinc-600">暂无数据</div>
      <div v-else class="space-y-4">
        <el-tabs>
          <el-tab-pane v-if="resumeCritique || jobCritique || hollandRiasec" label="AI 分析与霍兰德">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="mb-4"
              title="说明"
              description="当前后端在未解析 PDF/Word 正文时，会结合文件名与大小调用大模型生成指导性分析与 RIASEC 估计；上传真实解析文本后可进一步提高准确度。"
            />
            <el-card v-if="resumeCritique" shadow="never" class="mb-4">
              <div class="text-sm font-semibold text-zinc-700">简历优劣与改进建议</div>
              <pre class="mt-3 whitespace-pre-wrap rounded-lg bg-zinc-50 p-4 text-sm text-zinc-800">{{ resumeCritique }}</pre>
            </el-card>
            <el-card v-if="jobCritique" shadow="never" class="mb-4">
              <div class="text-sm font-semibold text-zinc-700">岗位需求智能解读</div>
              <pre class="mt-3 whitespace-pre-wrap rounded-lg bg-zinc-50 p-4 text-sm text-zinc-800">{{ jobCritique }}</pre>
            </el-card>
            <el-card v-if="hollandRows.length" shadow="never">
              <div class="text-sm font-semibold text-zinc-700">霍兰德 RIASEC（0–100）</div>
              <el-table class="mt-3" :data="hollandRows" size="small" border>
                <el-table-column prop="code" label="维度" width="90" />
                <el-table-column prop="label" label="类型" width="120" />
                <el-table-column prop="value" label="得分" />
              </el-table>
            </el-card>
          </el-tab-pane>
          <el-tab-pane label="结构化字段">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="状态">{{ data.status }}</el-descriptions-item>
            </el-descriptions>

            <div class="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
              <el-card shadow="never" class="lg:col-span-1">
                <div class="text-sm font-semibold text-zinc-700">技能</div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <el-tag v-for="s in skills" :key="s" type="success">{{ s }}</el-tag>
                  <div v-if="skills.length === 0" class="text-sm text-zinc-600">暂无</div>
                </div>
              </el-card>
              <el-card shadow="never" class="lg:col-span-2">
                <div class="text-sm font-semibold text-zinc-700">教育经历</div>
                <div class="mt-3 space-y-2">
                  <div v-for="(e, idx) in educations" :key="idx" class="rounded-lg border border-zinc-200 bg-white p-3">
                    <div class="text-sm text-zinc-800">{{ e.school || '未知学校' }}</div>
                    <div class="mt-1 text-xs text-zinc-500">{{ [e.degree, e.major].filter(Boolean).join(' / ') }}</div>
                  </div>
                  <div v-if="educations.length === 0" class="text-sm text-zinc-600">暂无</div>
                </div>
              </el-card>
            </div>

            <el-card shadow="never" class="mt-4">
              <div class="text-sm font-semibold text-zinc-700">项目经历</div>
              <div class="mt-3 space-y-2">
                <div v-for="(p, idx) in projects" :key="idx" class="rounded-lg border border-zinc-200 bg-white p-3">
                  <div class="text-sm text-zinc-800">{{ p.name || '未命名项目' }}</div>
                  <div class="mt-1 text-sm text-zinc-600">{{ p.summary || '暂无描述' }}</div>
                </div>
                <div v-if="projects.length === 0" class="text-sm text-zinc-600">暂无</div>
              </div>
            </el-card>

            <div class="mt-4">
              <el-collapse>
                <el-collapse-item title="解析结果（JSON）" name="json">
                  <pre class="max-h-[420px] overflow-auto rounded-lg bg-zinc-950 p-4 text-xs text-zinc-100">{{
                    JSON.stringify(data.resultJson, null, 2)
                  }}</pre>
                </el-collapse-item>
                <el-collapse-item title="证据（如有）" name="evidence">
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
