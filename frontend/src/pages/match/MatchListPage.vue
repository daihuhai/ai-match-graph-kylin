<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { unpublishResumeFromTalentPool, publishResumeToTalentPool } from '@/api/document'
import { recommendCandidates, recommendJobs } from '@/api/match'
import type { MatchListItem } from '@/types/match'
import { useMatchStore } from '@/stores/match'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { useDocumentStore } from '@/stores/document'
import AppEmpty from '@/components/AppEmpty.vue'
import { Briefcase, Building2, Search, SlidersHorizontal, RefreshCw, Star, History, Bookmark, ArrowRight, Eye, Send, RotateCcw } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const matchStore = useMatchStore()
const auth = useAuthStore()
const audit = useAuditStore()
const docsStore = useDocumentStore()

const isCompany = computed(() => route.path.startsWith('/company'))
const isPerson = computed(() => !isCompany.value)
const title = computed(() => (isCompany.value ? '人才库推荐' : '人才市场 · 职位推荐'))
const list = ref<MatchListItem[]>([])
const loading = ref(true)
const tab = ref<'recommend' | 'favorite' | 'history'>('recommend')
const minMatch = ref(0)

const poolDialog = ref(false)
const poolLoading = ref(false)
const activeRow = ref<MatchListItem | null>(null)
const poolForm = reactive({
  docId: '',
})

const userKey = computed(() => `${auth.userType ?? 'ANON'}:${auth.userId || 'anon'}`)
const favoriteSet = computed(() => matchStore.favoriteSet(userKey.value))
const historyList = computed(() => matchStore.historyByUser(userKey.value).filter((h) => (isCompany.value ? h.side === 'COMPANY' : h.side === 'PERSON')))

const favoriteList = computed(() => {
  const ids = favoriteSet.value
  const byId = new Map(list.value.map((x) => [x.recordId, x]))
  const stored = matchStore.historyByUser(userKey.value).filter((h) => ids.has(h.recordId))
  const merged = new Map<string, MatchListItem>()
  for (const x of stored) merged.set(x.recordId, x)
  for (const id of ids) {
    const r = byId.get(id)
    if (r) merged.set(id, r)
  }
  return Array.from(merged.values())
})

const resumeVersions = computed(() =>
  docsStore.docs
    .filter((d) => d.docType === 'RESUME' && d.status === 'DONE')
    .map((d) => ({
      id: d.id,
      fileName: d.fileName,
      createdAt: d.createdAt,
      deliveredCompanyAccounts: docsStore.results[d.id]?.deliveredCompanyAccounts ?? [],
    })),
)

const activeCompanyAccount = computed(() => activeRow.value?.companyAccount?.trim() || '')

const deliveredVersionsForActiveCompany = computed(() => {
  const companyAccount = activeCompanyAccount.value
  if (!companyAccount) return []
  return resumeVersions.value.filter((doc) => doc.deliveredCompanyAccounts.includes(companyAccount))
})

const selectedVersionDelivered = computed(() => {
  const companyAccount = activeCompanyAccount.value
  if (!companyAccount || !poolForm.docId) return false
  return (docsStore.results[poolForm.docId]?.deliveredCompanyAccounts ?? []).includes(companyAccount)
})

const selectedVersionLabel = computed(() => {
  const found = resumeVersions.value.find((doc) => doc.id === poolForm.docId)
  return found ? `${found.fileName} · ${found.createdAt}` : ''
})

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

const syncDeliveredCompany = (docId: string, companyAccount: string, publish: boolean) => {
  const current = docsStore.results[docId]?.deliveredCompanyAccounts ?? []
  const next = new Set(current)
  if (publish) next.add(companyAccount)
  else next.delete(companyAccount)
  docsStore.setDeliveredCompanyAccounts(docId, [...next])
}

const deliveredCountForCompany = (companyAccount?: string) => {
  if (!companyAccount) return 0
  return resumeVersions.value.filter((doc) => doc.deliveredCompanyAccounts.includes(companyAccount)).length
}

const openPoolDialog = (row: MatchListItem) => {
  activeRow.value = row
  const companyAccount = row.companyAccount?.trim() || ''
  const delivered = resumeVersions.value.filter((doc) => companyAccount && doc.deliveredCompanyAccounts.includes(companyAccount))
  poolForm.docId = delivered[0]?.id || resumeVersions.value[0]?.id || ''
  poolDialog.value = true
}

const submitPoolChange = async (publish: boolean) => {
  const row = activeRow.value
  const companyAccount = row?.companyAccount?.trim() || ''
  if (!row || !companyAccount) return ElMessage.warning('当前岗位缺少企业信息，暂时不能操作')
  if (!poolForm.docId) return ElMessage.warning('请先选择一个简历版本')
  poolLoading.value = true
  try {
    if (publish) {
      await publishResumeToTalentPool(poolForm.docId, companyAccount)
      syncDeliveredCompany(poolForm.docId, companyAccount, true)
      ElMessage.success('已投入所选简历版本')
    } else {
      await unpublishResumeFromTalentPool(poolForm.docId, companyAccount)
      syncDeliveredCompany(poolForm.docId, companyAccount, false)
      ElMessage.success('已取出所选简历版本')
    }
    audit.hydrate()
    audit.add({
      module: publish ? 'match.resume.putin' : 'match.resume.takeout',
      result: 'OK',
      detail: { companyAccount, docId: poolForm.docId, recordId: row.recordId },
    })
  } catch (e: any) {
    audit.hydrate()
    audit.add({
      module: publish ? 'match.resume.putin' : 'match.resume.takeout',
      result: 'FAIL',
      detail: { companyAccount, docId: poolForm.docId, recordId: row.recordId, message: e?.message || '操作失败' },
    })
    ElMessage.error(e?.message || '操作失败')
  } finally {
    poolLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    list.value = isCompany.value
      ? await recommendCandidates(minMatch.value)
      : await recommendJobs(minMatch.value)
    audit.hydrate()
    audit.add({
      module: 'match.recommend',
      result: 'OK',
      detail: { side: isCompany.value ? 'COMPANY' : 'PERSON', count: list.value.length },
    })
  } catch (e: any) {
    audit.hydrate()
    audit.add({
      module: 'match.recommend',
      result: 'FAIL',
      detail: { side: isCompany.value ? 'COMPANY' : 'PERSON', message: e?.message || '加载推荐失败' },
    })
    ElMessage.error(e?.message || '加载推荐失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
onMounted(() => {
  matchStore.hydrate()
  docsStore.hydrate()
})

const openDetail = (row: MatchListItem) => {
  const base = isCompany.value ? '/company' : '/person'
  matchStore.addHistory(row, isCompany.value ? 'COMPANY' : 'PERSON')
  audit.hydrate()
  audit.add({
    module: 'match.detail.open',
    result: 'OK',
    detail: { side: isCompany.value ? 'COMPANY' : 'PERSON', recordId: row.recordId },
  })
  router.push(`${base}/match/detail/${encodeURIComponent(row.recordId)}`)
}

const toggleFav = (recordId: string) => {
  matchStore.toggleFavorite(recordId)
  audit.hydrate()
  audit.add({ module: 'match.favorite.toggle', result: 'OK', detail: { recordId } })
}

const getScoreColor = (score: number) => {
  if (score >= 90) return 'text-emerald-600 bg-emerald-50 border-emerald-200'
  if (score >= 75) return 'text-blue-600 bg-blue-50 border-blue-200'
  if (score >= 60) return 'text-amber-600 bg-amber-50 border-amber-200'
  return 'text-app-text bg-app-bg border-app-border'
}

const getScoreProgressColor = (score: number) => {
  if (score >= 90) return '#10b981' // emerald-500
  if (score >= 75) return '#3b82f6' // blue-500
  if (score >= 60) return '#f59e0b' // amber-500
  return '#94a3b8' // slate-400
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header Card -->
    <div class="glass-panel rounded-3xl p-8">
      <div class="flex flex-col lg:flex-row justify-between gap-8 items-center">
        <div class="flex-1">
          <h2 class="text-3xl font-extrabold text-app-text dark:text-white flex items-center gap-3 tracking-tight">
            <div class="p-2.5 bg-indigo-50 dark:bg-indigo-500/10 rounded-xl text-indigo-600 dark:text-indigo-400">
              <Search class="w-6 h-6" />
            </div>
            {{ title }}
          </h2>
          <p class="mt-4 text-[15px] text-app-subtext dark:text-zinc-400 leading-relaxed max-w-2xl">
            基于大模型与霍兰德 RIASEC 理论，为您进行双向智能匹配。
            <template v-if="isPerson">从人才市场海量岗位中，发现最适合您的发展机会。您可以一键将简历投入感兴趣的企业人才库。</template>
            <template v-else>从全平台人才库中，为您推荐最匹配的候选人。</template>
          </p>
        </div>
        
        <div class="w-full lg:w-80 bg-app-panel/60 dark:bg-zinc-900/60 backdrop-blur rounded-2xl p-5 border border-app-border/50 dark:border-zinc-800/50 shadow-sm flex flex-col justify-center">
          <div class="flex items-center justify-between text-sm font-semibold text-app-text dark:text-zinc-300 mb-4">
            <span class="flex items-center gap-2"><SlidersHorizontal class="w-4 h-4 text-indigo-500" /> 最低匹配度过滤</span>
            <span class="text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-500/10 px-2 py-0.5 rounded-md">{{ minMatch }}%</span>
          </div>
          <el-slider v-model="minMatch" :min="0" :max="100" :step="1" :show-tooltip="false" class="!px-2" />
          <el-button type="primary" class="w-full mt-5 !h-10 !rounded-xl !bg-indigo-600 hover:!bg-indigo-700 !border-none shadow-md shadow-indigo-500/20 transition-all active:scale-[0.98]" :loading="loading" @click="load">
            <RefreshCw class="w-4 h-4 mr-2" :class="{'animate-spin': loading}" />
            应用过滤并刷新
          </el-button>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="bg-app-panel/60 dark:bg-zinc-900/40 backdrop-blur-sm rounded-3xl border border-app-border/50 dark:border-zinc-800/50 shadow-sm overflow-hidden">
      <!-- Custom Tabs -->
      <div class="flex border-b border-app-border/50 dark:border-zinc-800/50 px-4 pt-4 bg-app-bg/30 dark:bg-zinc-900/30">
        <button 
          v-for="t in [{id: 'recommend', label: '智能推荐', icon: Star}, {id: 'favorite', label: '我的收藏', icon: Bookmark}, {id: 'history', label: '浏览历史', icon: History}]" 
          :key="t.id"
          class="flex items-center gap-2 px-6 py-3.5 text-[15px] font-semibold border-b-2 transition-all relative outline-none"
          :class="tab === t.id ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400' : 'border-transparent text-app-subtext hover:text-app-text dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-app-bg/50 dark:hover:bg-zinc-800/50 rounded-t-xl'"
          @click="tab = t.id as any"
        >
          <component :is="t.icon" class="w-[18px] h-[18px]" :class="tab === t.id ? 'text-indigo-600 dark:text-indigo-400' : ''" />
          {{ t.label }}
        </button>
      </div>

      <!-- List Content -->
      <div class="p-6 sm:p-8 min-h-[400px]">
        <!-- Recommend Tab -->
        <template v-if="tab === 'recommend'">
          <AppEmpty v-if="!loading && list.length === 0" description="未找到满足当前匹配度的数据，请尝试调低过滤阈值。">
            <el-button type="primary" @click="load">刷新重试</el-button>
          </AppEmpty>
          
          <div v-else-if="loading" class="space-y-4">
            <div v-for="i in 3" :key="i" class="h-24 bg-app-bg dark:bg-slate-800 rounded-xl animate-pulse"></div>
          </div>
          
          <TransitionGroup name="list" tag="div" class="grid grid-cols-1 gap-4 relative" v-else>
            <div v-for="row in list" :key="row.recordId" class="flex flex-col sm:flex-row items-start sm:items-center justify-between p-5 rounded-xl border border-app-border dark:border-slate-700 hover:border-indigo-300 dark:hover:border-indigo-500 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300 group bg-app-panel dark:bg-slate-800">
              <div class="flex-1 min-w-0 pr-4">
                <div class="flex items-center gap-3 mb-2">
                  <h3 class="text-lg font-bold text-app-text dark:text-white truncate" :title="row.title">{{ row.title }}</h3>
                  <el-tag v-if="isPerson && deliveredCountForCompany(row.companyAccount)" type="success" effect="light" round size="small" class="!border-emerald-200">
                    已投递 {{ deliveredCountForCompany(row.companyAccount) }} 份
                  </el-tag>
                </div>
                <div class="flex items-center text-sm text-app-subtext dark:text-app-subtext gap-4">
                  <span class="flex items-center gap-1.5 truncate max-w-xs" :title="row.org">
                    <Building2 class="w-4 h-4 text-app-subtext" />
                    {{ row.org }}
                  </span>
                  <span v-if="isPerson && row.companyAccount" class="hidden sm:inline-block px-2 py-0.5 rounded-md bg-app-bg dark:bg-slate-700 text-xs font-mono">
                    ID: {{ row.companyAccount }}
                  </span>
                </div>
              </div>
              
              <div class="flex items-center gap-6 mt-4 sm:mt-0 w-full sm:w-auto shrink-0 justify-between sm:justify-end">
                <div class="flex flex-col items-center">
                  <div class="text-xs text-app-subtext mb-1">匹配度</div>
                  <div class="flex items-center justify-center w-12 h-12 rounded-full border-2" :class="getScoreColor(row.score)">
                    <span class="text-sm font-bold">{{ row.score }}</span>
                  </div>
                </div>
                
                <div class="flex items-center gap-2 border-l border-app-border dark:border-slate-700 pl-6">
                  <button 
                    @click="toggleFav(row.recordId)" 
                    class="p-2 rounded-lg transition-colors"
                    :class="favoriteSet.has(row.recordId) ? 'text-amber-500 bg-amber-50 hover:bg-amber-100 dark:bg-amber-900/30' : 'text-app-subtext hover:text-app-text hover:bg-app-bg dark:hover:bg-slate-700'"
                    :title="favoriteSet.has(row.recordId) ? '取消收藏' : '收藏'"
                  >
                    <Bookmark class="w-5 h-5" :class="{'fill-current': favoriteSet.has(row.recordId)}" />
                  </button>
                  
                  <el-button v-if="isPerson" class="!px-3" :disabled="resumeVersions.length === 0 || !row.companyAccount" @click="openPoolDialog(row)">
                    投递管理
                  </el-button>
                  
                  <el-button type="primary" class="!bg-indigo-600 hover:!bg-indigo-700 !border-none !px-4" @click="openDetail(row)">
                    匹配详情 <ArrowRight class="w-4 h-4 ml-1" />
                  </el-button>
                </div>
              </div>
            </div>
          </TransitionGroup>
        </template>

        <!-- Favorite Tab -->
        <template v-else-if="tab === 'favorite'">
          <AppEmpty v-if="favoriteList.length === 0" description="您还没有收藏任何推荐项。">
            <el-button type="primary" @click="tab = 'recommend'">去发现更多</el-button>
          </AppEmpty>
          
          <TransitionGroup name="list" tag="div" class="grid grid-cols-1 gap-4 relative" v-else>
            <div v-for="row in favoriteList" :key="row.recordId" class="flex flex-col sm:flex-row items-start sm:items-center justify-between p-5 rounded-xl border border-amber-200 dark:border-amber-900/50 hover:border-amber-400 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300 bg-amber-50/30 dark:bg-amber-900/10">
              <div class="flex-1 min-w-0 pr-4">
                <div class="flex items-center gap-3 mb-2">
                  <h3 class="text-lg font-bold text-app-text dark:text-white truncate" :title="row.title">{{ row.title }}</h3>
                  <el-tag v-if="isPerson && deliveredCountForCompany(row.companyAccount)" type="success" effect="light" round size="small">已投递</el-tag>
                </div>
                <div class="flex items-center text-sm text-app-subtext gap-2">
                  <Building2 class="w-4 h-4" />
                  <span class="truncate" :title="row.org">{{ row.org }}</span>
                </div>
              </div>
              
              <div class="flex items-center gap-6 mt-4 sm:mt-0 w-full sm:w-auto shrink-0 justify-between sm:justify-end">
                <div class="px-3 py-1 rounded-full border text-sm font-bold" :class="getScoreColor(row.score)">
                  {{ row.score }}分
                </div>
                
                <div class="flex items-center gap-2">
                  <el-button type="danger" plain size="small" @click="toggleFav(row.recordId)">取消收藏</el-button>
                  <el-button type="primary" size="small" class="!bg-indigo-600" @click="openDetail(row)">查看详情</el-button>
                </div>
              </div>
            </div>
          </TransitionGroup>
        </template>

        <!-- History Tab -->
        <template v-else>
          <AppEmpty v-if="historyList.length === 0" description="暂无浏览历史记录。" />
          
          <TransitionGroup name="list" tag="div" class="grid grid-cols-1 gap-4 relative" v-else>
            <div v-for="row in historyList" :key="row.recordId" class="flex flex-col sm:flex-row items-start sm:items-center justify-between p-4 rounded-xl border border-app-border dark:border-slate-800 bg-app-bg/50 dark:bg-slate-800/50 hover:bg-app-panel dark:hover:bg-slate-800 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
              <div class="flex-1 min-w-0 pr-4">
                <div class="flex items-center gap-2 mb-1">
                  <h4 class="font-medium text-app-text dark:text-slate-200 truncate">{{ row.title }}</h4>
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold border" :class="getScoreColor(row.score)">{{ row.score }}分</span>
                </div>
                <div class="text-xs text-app-subtext flex items-center gap-4">
                  <span class="truncate max-w-[200px]">{{ row.org }}</span>
                  <span class="flex items-center gap-1"><History class="w-3 h-3" /> {{ fmt(row.viewedAt) }}</span>
                </div>
              </div>
              
              <div class="mt-3 sm:mt-0 flex gap-2">
                <el-button size="small" @click="openDetail(row)">再次查看</el-button>
              </div>
            </div>
          </TransitionGroup>
        </template>
      </div>
    </div>
  </div>

  <!-- Delivery Dialog -->
  <el-dialog v-model="poolDialog" title="简历投递管理" width="500px" class="!rounded-2xl" destroy-on-close>
    <div class="space-y-5">
      <!-- Target Info -->
      <div class="flex items-center gap-4 p-4 bg-indigo-50 dark:bg-indigo-900/20 rounded-xl border border-indigo-100 dark:border-indigo-800">
        <div class="w-12 h-12 rounded-lg bg-indigo-100 dark:bg-indigo-800 flex items-center justify-center shrink-0">
          <Building2 class="w-6 h-6 text-indigo-600 dark:text-indigo-400" />
        </div>
        <div class="min-w-0 flex-1">
          <div class="font-bold text-app-text dark:text-white truncate text-base">{{ activeRow?.title || '-' }}</div>
          <div class="text-sm text-app-text dark:text-app-subtext truncate mt-0.5">{{ activeRow?.org || '-' }}</div>
        </div>
      </div>

      <AppEmpty v-if="resumeVersions.length === 0" description="文档中心暂无已解析完成的简历。">
        <el-button type="primary" @click="router.push('/person/doc/list')">去上传简历</el-button>
      </AppEmpty>

      <template v-else>
        <!-- Version Selection -->
        <div class="space-y-2">
          <label class="text-sm font-semibold text-app-text dark:text-slate-200">选择要操作的简历版本</label>
          <el-select v-model="poolForm.docId" class="w-full" placeholder="请选择简历版本" size="large">
            <el-option
              v-for="doc in resumeVersions"
              :key="doc.id"
              :label="`${doc.fileName}`"
              :value="doc.id"
            >
              <div class="flex justify-between items-center w-full">
                <span>{{ doc.fileName }}</span>
                <span class="text-xs text-app-subtext">{{ doc.createdAt.split('T')[0] }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- Status Card -->
        <div v-if="poolForm.docId" class="p-4 rounded-xl border border-app-border dark:border-slate-700 bg-app-panel dark:bg-slate-800">
          <div class="flex items-center justify-between">
            <span class="text-sm text-app-text dark:text-app-subtext">当前版本投递状态：</span>
            <el-tag :type="selectedVersionDelivered ? 'success' : 'info'" effect="dark" round>
              {{ selectedVersionDelivered ? '已投递给该企业' : '尚未投递' }}
            </el-tag>
          </div>
        </div>

        <!-- History Info -->
        <div v-if="deliveredVersionsForActiveCompany.length" class="pt-2">
          <div class="text-xs text-app-subtext mb-2">已投递给该企业的其他版本：</div>
          <div class="flex flex-wrap gap-2">
            <el-tag v-for="doc in deliveredVersionsForActiveCompany" :key="doc.id" type="info" size="small" class="!bg-app-bg !text-app-text">
              {{ doc.fileName }}
            </el-tag>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3 pt-4 border-t border-app-border dark:border-slate-800 mt-4">
        <el-button @click="poolDialog = false" size="large" class="!rounded-lg">取消</el-button>
        
        <el-button
          v-if="selectedVersionDelivered"
          type="danger"
          size="large"
          class="!rounded-lg !px-6"
          :loading="poolLoading"
          @click="submitPoolChange(false)"
        >
          <RotateCcw class="w-4 h-4 mr-2" /> 撤回简历
        </el-button>
        
        <el-button
          v-else
          type="primary"
          size="large"
          class="!rounded-lg !bg-indigo-600 hover:!bg-indigo-700 !border-none !px-6"
          :disabled="resumeVersions.length === 0 || !poolForm.docId"
          :loading="poolLoading"
          @click="submitPoolChange(true)"
        >
          <Send class="w-4 h-4 mr-2" /> 确认投递
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>
