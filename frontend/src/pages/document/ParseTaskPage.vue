<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDocumentStatus } from '@/api/document'
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
const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))

const step = computed(() => {
  if (status.value === 'PENDING') return 1
  if (status.value === 'PROCESSING') return 2
  return 3
})

const fetchStatus = async () => {
  try {
    const resp = await getDocumentStatus(docId.value)
    status.value = String((resp as any).status) as DocStatus
    loading.value = false
    docsStore.hydrate()
    docsStore.updateStatus(docId.value, status.value)
    if (status.value === 'DONE' || status.value === 'FAILED') {
      audit.hydrate()
      audit.add({
        module: 'document.parse',
        result: status.value === 'DONE' ? 'OK' : 'FAIL',
        detail: { docId: docId.value, status: status.value },
      })
      poll.stop()
    }
  } catch (e: any) {
    loading.value = false
    poll.stop()
    ElMessage.error(e?.message || '查询状态失败')
  }
}

const poll = usePolling(fetchStatus, 1200)

onMounted(async () => {
  docsStore.hydrate()
  await poll.start()
})

const goResult = () => router.push(`${base.value}/doc/result/${encodeURIComponent(docId.value)}`)
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
          <el-step title="排队" description="任务已进入队列" />
          <el-step title="解析中" description="正在抽取结构化信息" />
          <el-step title="完成" description="可查看结果并继续操作" />
        </el-steps>

        <div v-if="loading" class="text-sm text-zinc-600">正在获取状态...</div>
        <div v-else-if="status === 'FAILED'" class="text-sm text-zinc-600">解析失败，请重新上传或稍后重试。</div>
        <div v-else-if="status === 'DONE'" class="text-sm text-zinc-600">
          解析已完成。个人端可在结果页选择企业投递，企业端只能看到投给自己的简历。
        </div>
        <div v-else class="text-sm text-zinc-600">状态会自动刷新。</div>

        <div class="flex justify-end gap-2">
          <el-button @click="router.push(`${base}/doc/list`)">返回文档中心</el-button>
          <el-button type="primary" :disabled="status !== 'DONE'" @click="goResult">查看结果</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>
