<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadDocument } from '@/api/document'
import type { DocType } from '@/types/document'
import { useDocumentStore } from '@/stores/document'
import { useAuditStore } from '@/stores/audit'

const route = useRoute()
const router = useRouter()
const docsStore = useDocumentStore()
const audit = useAuditStore()

const isCompany = computed(() => route.path.startsWith('/company'))
const docType = computed<DocType>(() => (isCompany.value ? 'JOB_DESC' : 'RESUME'))
const title = computed(() => (isCompany.value ? 'JD 上传' : '简历上传'))

const file = ref<File | null>(null)
const fileList = ref<any[]>([])
const authorized = ref(false)
const loading = ref(false)

const validateFile = (raw: File) => {
  const okType = /\.(pdf|doc|docx)$/i.test(raw.name)
  const okSize = raw.size <= 50 * 1024 * 1024
  if (!okType) ElMessage.warning('仅支持 PDF/DOC/DOCX')
  if (!okSize) ElMessage.warning('文件大小需 ≤ 50MB')
  return okType && okSize
}

const onChange = (uploadFile: any, uploadFiles: any[]) => {
  const raw = uploadFile?.raw as File | undefined
  if (!raw) return
  if (!validateFile(raw)) {
    file.value = null
    fileList.value = []
    return
  }
  file.value = raw
  fileList.value = uploadFiles.slice(-1)
}

const onRemove = () => {
  file.value = null
  fileList.value = []
}

const submit = async () => {
  if (!authorized.value) return ElMessage.warning('请先勾选授权确认')
  if (!file.value) return ElMessage.warning('请先选择文件')

  loading.value = true
  try {
    const { docId } = await uploadDocument({ file: file.value, docType: docType.value })
    audit.hydrate()
    audit.add({
      module: 'document.upload',
      result: 'OK',
      detail: { docId, docType: docType.value, fileName: file.value.name, size: file.value.size },
    })
    docsStore.hydrate()
    docsStore.addDoc({
      id: docId,
      fileName: file.value.name,
      fileType: file.value.name.toLowerCase().endsWith('.pdf') ? 'PDF' : 'DOC',
      docType: docType.value,
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    })
    ElMessage.success('上传成功，已进入解析任务')
    const base = isCompany.value ? '/company' : '/person'
    await router.push(`${base}/doc/task/${encodeURIComponent(docId)}`)
  } catch (e: any) {
    audit.hydrate()
    audit.add({
      module: 'document.upload',
      result: 'FAIL',
      detail: { docType: docType.value, fileName: file.value?.name, message: e?.message || '上传失败' },
    })
    ElMessage.error(e?.message || '上传失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">{{ title }}</div>
          <div class="mt-1 text-sm text-zinc-600">
            支持 PDF/DOC/DOCX，最大 50MB。上传后会自动进入解析进度跟踪。
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="space-y-4">
        <el-alert type="info" show-icon :closable="false">
          <template #title>合规提示</template>
          <template #default>
            请确认已获得文档主体授权，不上传非法采集的个人隐私数据。未勾选授权将无法提交。
          </template>
        </el-alert>

        <el-checkbox v-model="authorized">
          本人/本单位已获得该文档相关主体授权，并同意隐私条款与数据使用说明
        </el-checkbox>

        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="true"
          :multiple="false"
          :limit="1"
          accept=".pdf,.doc,.docx"
          :file-list="fileList"
          :on-change="onChange"
          :on-remove="onRemove"
        >
          <div class="py-8">
            <div class="text-sm font-semibold text-zinc-900">拖拽文件到此处，或点击选择</div>
            <div class="mt-1 text-xs text-zinc-500">无后端时默认走 mock；设置 VITE_USE_MOCK=false 可切到真实接口</div>
          </div>
        </el-upload>

        <div class="flex justify-end">
          <el-button type="primary" :loading="loading" @click="submit">提交并解析</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>
