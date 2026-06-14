<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDocumentStore } from '@/stores/document'
import type { DocStatus, DocFileVO } from '@/types/document'
import AppEmpty from '@/components/AppEmpty.vue'
import { FileText, Plus, Clock, FileUp, Activity, CheckCircle, XCircle, ChevronRight, Eye, Trash2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  return docsStore.docs.filter((d) => d.docType === t).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
})

const statusTag = (s: DocStatus) => {
  if (s === 'DONE') return { type: 'success', label: '解析完成', icon: CheckCircle, class: 'bg-emerald-50 text-emerald-600 border-emerald-200' }
  if (s === 'FAILED') return { type: 'danger', label: '解析失败', icon: XCircle, class: 'bg-rose-50 text-rose-600 border-rose-200' }
  if (s === 'PROCESSING') return { type: 'warning', label: 'AI 分析中', icon: Activity, class: 'bg-amber-50 text-amber-600 border-amber-200' }
  if (s === 'PENDING') return { type: 'info', label: '排队中', icon: Clock, class: 'bg-app-bg text-app-text border-app-border' }
  if (s === 'UPLOADING') return { type: 'info', label: '上传中', icon: FileUp, class: 'bg-indigo-50 text-indigo-600 border-indigo-200' }
  return { type: 'info', label: s, icon: FileText, class: 'bg-app-bg text-app-text border-app-border' }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

const getFileIconColor = (type: string) => {
  const ext = type?.toLowerCase() || ''
  if (ext.includes('pdf')) return 'text-rose-500'
  if (ext.includes('doc')) return 'text-blue-500'
  return 'text-app-subtext'
}

const goUpload = () => router.push(`${base.value}/doc/upload`)
const goTask = (docId: string) => router.push(`${base.value}/doc/task/${encodeURIComponent(docId)}`)
const goResult = (docId: string) => router.push(`${base.value}/doc/result/${encodeURIComponent(docId)}`)

const selectedIds = ref<Set<string>>(new Set())

const toggleSelect = (docId: string) => {
  if (selectedIds.value.has(docId)) {
    selectedIds.value.delete(docId)
  } else {
    selectedIds.value.add(docId)
  }
}

const selectAll = () => {
  if (selectedIds.value.size === list.value.length) {
    selectedIds.value.clear()
  } else {
    list.value.forEach(doc => selectedIds.value.add(doc.id))
  }
}

const deleteDoc = async (doc: DocFileVO) => {
  await ElMessageBox.confirm(`确认删除文档 "${doc.fileName}"？`, '提示', { type: 'warning' })
  try {
    await docsStore.deleteDoc(doc.id)
    selectedIds.value.delete(doc.id)
    ElMessage.success('删除成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const batchDelete = async () => {
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请先选择要删除的文档')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.size} 个文档？`, '提示', { type: 'warning' })
  try {
    for (const id of selectedIds.value) {
      await docsStore.deleteDoc(id)
    }
    selectedIds.value.clear()
    ElMessage.success('批量删除成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold text-app-text">{{ title }}</h2>
        <p class="mt-1 text-sm text-app-subtext">管理您的文档上传记录、追踪 AI 解析进度，并查看能力图谱抽取结果。</p>
      </div>
      <div class="flex items-center gap-3">
        <el-button 
          v-if="selectedIds.size > 0" 
          type="danger" 
          size="large" 
          class="!bg-rose-500 hover:!bg-rose-600 !border-none" 
          @click="batchDelete"
        >
          <Trash2 class="w-4 h-4 mr-1" /> 删除选中 ({{ selectedIds.size }})
        </el-button>
        <el-button type="primary" size="large" class="!bg-indigo-600 hover:!bg-indigo-700 !border-none shadow-md shadow-indigo-200" @click="goUpload">
          <Plus class="w-4 h-4 mr-1" /> 新增上传
        </el-button>
      </div>
    </div>

    <div v-if="list.length === 0" class="flex flex-col items-center justify-center py-20 px-4 bg-app-panel rounded-2xl border border-app-border border-dashed">
      <div class="w-20 h-20 bg-indigo-50 rounded-full flex items-center justify-center mb-4">
        <FileUp class="w-10 h-10 text-indigo-500" />
      </div>
      <h3 class="text-lg font-medium text-app-text mb-2">暂无文档记录</h3>
      <p class="text-app-subtext text-center max-w-sm mb-6">您还没有上传任何文档。请点击上方按钮上传您的{{ isCompany ? '岗位需求(JD)' : '个人简历' }}。</p>
      <el-button type="primary" @click="goUpload" class="!px-6">立即上传</el-button>
    </div>
    
    <div v-else>
      <div class="flex items-center gap-3 mb-4">
        <el-checkbox 
          :checked="selectedIds.size === list.length && list.length > 0" 
          @change="selectAll"
          class="!text-app-text"
        />
        <span class="text-sm text-app-subtext">全选</span>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        <div 
          v-for="doc in list" 
          :key="doc.id" 
          class="bg-app-panel rounded-xl border border-app-border hover:border-indigo-300 hover:shadow-lg hover:shadow-indigo-100/50 transition-all duration-300 group overflow-hidden flex flex-col"
          :class="{ 'border-indigo-400 bg-indigo-50/30': selectedIds.has(doc.id) }"
        >
          <!-- Card Header -->
          <div class="p-5 border-b border-app-border flex items-start gap-4 flex-1">
            <el-checkbox 
              :checked="selectedIds.has(doc.id)" 
              @change="toggleSelect(doc.id)"
              class="shrink-0 mt-3"
            />
            <div class="p-3 rounded-lg bg-app-bg shrink-0">
              <FileText class="w-8 h-8" :class="getFileIconColor(doc.fileType)" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-app-text truncate" :title="doc.fileName">{{ doc.fileName }}</h4>
              <div class="flex flex-wrap items-center mt-2 gap-2 text-xs text-app-subtext">
                <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium border uppercase" :class="statusTag(doc.status).class">
                  <component :is="statusTag(doc.status).icon" class="w-3 h-3 mr-1" />
                  {{ statusTag(doc.status).label }}
                </span>
                <span class="truncate">{{ formatDate(doc.createdAt) }}</span>
              </div>
            </div>
          </div>
          
          <!-- Card Actions -->
          <div class="bg-app-bg/50 p-3 flex justify-between items-center px-5">
            <span class="text-xs text-app-subtext font-mono font-medium">{{ doc.fileType?.toUpperCase() || 'UNKNOWN' }}</span>
            <div class="flex space-x-2">
              <el-button v-if="doc.status !== 'DONE'" size="small" type="primary" plain class="!bg-indigo-50 !text-indigo-600 hover:!bg-indigo-600 hover:!text-white border-none" @click="goTask(doc.id)">
                查看进度 <ChevronRight class="w-3 h-3 ml-1" />
              </el-button>
              <template v-else>
                <el-button size="small" class="!text-app-text hover:!text-indigo-600" text @click="goTask(doc.id)">详情</el-button>
                <el-button size="small" type="primary" class="!bg-indigo-600 hover:!bg-indigo-700 !border-none" @click="goResult(doc.id)">
                  <Eye class="w-3 h-3 mr-1" /> 结构化结果
                </el-button>
                <el-button size="small" type="danger" text class="!text-rose-500 hover:!text-rose-600" @click="deleteDoc(doc)">
                  <Trash2 class="w-3 h-3 mr-1" /> 删除
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
