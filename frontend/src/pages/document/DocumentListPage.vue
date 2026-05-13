<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDocumentStore } from '@/stores/document'
import type { DocStatus } from '@/types/document'
import AppEmpty from '@/components/AppEmpty.vue'

const route = useRoute()
const router = useRouter()
const docsStore = useDocumentStore()

const isCompany = computed(() => route.path.startsWith('/company'))
const base = computed(() => (isCompany.value ? '/company' : '/person'))
const title = computed(() => (isCompany.value ? 'JD 文档中心' : '简历文档中心'))

onMounted(() => {
  docsStore.hydrate()
})

const list = computed(() => {
  const t = isCompany.value ? 'JOB_DESC' : 'RESUME'
  return docsStore.docs.filter((d) => d.docType === t)
})

const statusTag = (s: DocStatus) => {
  if (s === 'DONE') return { type: 'success', label: '完成' }
  if (s === 'FAILED') return { type: 'danger', label: '失败' }
  if (s === 'PROCESSING') return { type: 'warning', label: '处理中' }
  if (s === 'PENDING') return { type: 'info', label: '排队中' }
  if (s === 'UPLOADING') return { type: 'info', label: '上传中' }
  return { type: 'info', label: s }
}

const goUpload = () => router.push(`${base.value}/doc/upload`)
const goTask = (docId: string) => router.push(`${base.value}/doc/task/${encodeURIComponent(docId)}`)
const goResult = (docId: string) => router.push(`${base.value}/doc/result/${encodeURIComponent(docId)}`)
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div class="text-base font-semibold">{{ title }}</div>
          <div class="mt-1 text-sm text-zinc-600">包含上传记录、解析任务与结果入口。</div>
        </div>
        <el-button type="primary" @click="goUpload">新增上传</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <AppEmpty v-if="list.length === 0" description="暂无记录，请先上传文档。">
        <el-button type="primary" @click="goUpload">去上传</el-button>
      </AppEmpty>
      <el-table v-else :data="list">
        <el-table-column prop="fileName" label="文件名" min-width="260" />
        <el-table-column prop="fileType" label="类型" width="90" />
        <el-table-column prop="createdAt" label="创建时间" width="200" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type">{{ statusTag(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goTask(row.id)">任务</el-button>
            <el-button link type="primary" :disabled="row.status !== 'DONE'" @click="goResult(row.id)">结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
