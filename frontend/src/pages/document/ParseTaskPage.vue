<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDocumentStatus, getParseResult, publishResumeToTalentPool } from '@/api/document'
import { usePolling } from '@/composables/usePolling'
import { useDocumentStore } from '@/stores/document'
import { useAuditStore } from '@/stores/audit'

type DocStatus = 'UPLOADING' | 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED'

const route = useRoute()
const router = useRouter()
const docsStore = useDocumentStore()
const audit = useAuditStore()

const docId = computed(() => String(route.params.docId || ''))
const status = ref<DocStatus>('PENDING')
const loading = ref(true)

const poolLoading = ref(false)
const poolCanPublish = ref(false)
const poolPublished = ref(false)

const step = computed(() => {
  if (status.value === 'PENDING') return 1
  if (status.value === 'PROCESSING') return 2
  if (status.value === 'DONE') return 3
  if (status.value === 'FAILED') return 3
  return 1
})

const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))

const isResumeDoc = computed(() => docsStore.docs.find((d) => d.id === docId.value)?.docType === 'RESUME')

const loadPoolMeta = async () => {
  if (isCompany.value || !isResumeDoc.value || !docId.value) return
  try {
    const r = await getParseResult(docId.value)
    poolCanPublish.value = !!r.canPublishToTalentPool
    poolPublished.value = !!r.talentPoolPublished
    docsStore.hydrate()
    docsStore.setResult(docId.value, r)
    docsStore.updateStatus(docId.value, r.status)
  } catch {
    poolCanPublish.value = false
    poolPublished.value = false
  }
}

watch(
  () => [status.value, isResumeDoc.value, isCompany.value] as const,
  async ([st]) => {
    if (st === 'DONE' && !isCompany.value && isResumeDoc.value) {
      await loadPoolMeta()
    }
  },
)

const fetchStatus = async () => {
  try {
    const resp = await getDocumentStatus(docId.value)
    const s = String((resp as any).status) as DocStatus
    status.value = s
    loading.value = false
    docsStore.hydrate()
    docsStore.updateStatus(docId.value, s)
    if (s === 'DONE' || s === 'FAILED') {
      audit.hydrate()
      audit.add({
        module: 'document.parse',
        result: s === 'DONE' ? 'OK' : 'FAIL',
        detail: { docId: docId.value, status: s },
      })
    }
    if (s === 'DONE' || s === 'FAILED') poll.stop()
  } catch (e: any) {
    loading.value = false
    poll.stop()
    audit.hydrate()
    audit.add({
      module: 'document.parse',
      result: 'FAIL',
      detail: { docId: docId.value, message: e?.message || '查询状态失败' },
    })
    ElMessage.error(e?.message || '查询状态失败')
  }
}

const poll = usePolling(fetchStatus, 1200)

onMounted(async () => {
  docsStore.hydrate()
  await poll.start()
})

const goResult = () => router.push(`${base.value}/doc/result/${encodeURIComponent(docId.value)}`)

const publishToPool = async () => {
  poolLoading.value = true
  try {
    await publishResumeToTalentPool(docId.value)
    ElMessage.success('已上传至系统人才库，企业端可在「人才库推荐」中查看')
    await loadPoolMeta()
  } catch (e: any) {
    ElMessage.error(e?.message || '上传人才库失败')
  } finally {
    poolLoading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">解析任务</div>
          <div class="mt-1 text-sm text-zinc-600">DocId：{{ docId }}</div>
        </div>
        <div class="flex items-center gap-2">
          <el-tag v-if="status === 'DONE'" type="success">完成</el-tag>
          <el-tag v-else-if="status === 'FAILED'" type="danger">失败</el-tag>
          <el-tag v-else-if="status === 'PROCESSING'" type="warning">处理中</el-tag>
          <el-tag v-else type="info">排队中</el-tag>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="space-y-4">
        <el-steps :active="step" align-center>
          <el-step title="排队" description="任务进入队列" />
          <el-step title="解析中" description="抽取结构化信息" />
          <el-step title="完成" description="可查看结果与图谱" />
        </el-steps>

        <div v-if="loading" class="text-sm text-zinc-600">正在获取状态...</div>
        <div v-else-if="status === 'FAILED'" class="text-sm text-zinc-600">
          解析失败（演示）。你可以返回重新上传，或稍后重试。
        </div>
        <div v-else-if="status === 'DONE'" class="text-sm text-zinc-600">
          解析完成，可进入结果页查看结构化字段与证据。
        </div>
        <div v-else class="text-sm text-zinc-600">状态将自动刷新。</div>

        <el-card
          v-if="status === 'DONE' && !isCompany && isResumeDoc"
          shadow="never"
          class="border border-emerald-100 bg-emerald-50/40"
        >
          <div class="text-sm font-semibold text-zinc-800">人才库</div>
          <div class="mt-2 text-sm text-zinc-700">
            <span v-if="poolPublished">本简历已上传至系统人才库。</span>
            <span v-else-if="poolCanPublish">解析已完成，可将本简历上传至系统人才库，供企业在「人才库推荐」中筛选。</span>
            <span v-else>
              当前简历未绑定个人登录账号，无法写入人才库。请使用个人账号登录后重新上传，再在本页或结果页点击上传人才库。
            </span>
          </div>
          <div class="mt-3 flex flex-wrap justify-end gap-2">
            <el-button
              v-if="poolCanPublish && !poolPublished"
              type="success"
              :loading="poolLoading"
              @click="publishToPool"
            >
              上传人才库
            </el-button>
          </div>
        </el-card>

        <div class="flex justify-end gap-2">
          <el-button @click="router.push(`${base}/doc/list`)">返回文档中心</el-button>
          <el-button type="primary" :disabled="status !== 'DONE'" @click="goResult">查看结果</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>
