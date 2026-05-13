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
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">{{ title }}</div>
          <div class="mt-1 text-sm text-zinc-600">
            霍兰德 RIASEC：企业端从系统人才库匹配候选人；个人端从人才市场（含企业发布的 JD）匹配岗位。可设置最低匹配度。
          </div>
          <div v-if="isPerson" class="mt-3 rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 text-sm text-zinc-600">
            投入时可选择文档中心中的任一已完成简历版本；取出时按版本生效。
          </div>
          <div class="mt-4 flex max-w-md flex-col gap-2">
            <div class="flex items-center justify-between text-xs text-zinc-600">
              <span>最低匹配度</span>
              <span class="font-medium text-zinc-800">{{ minMatch }}%</span>
            </div>
            <el-slider v-model="minMatch" :min="0" :max="100" :step="1" show-stops :marks="{ 0: '0', 60: '60', 100: '100' }" />
          </div>
        </div>
        <el-button type="primary" :loading="loading" @click="load">应用并刷新</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="推荐" name="recommend" />
        <el-tab-pane label="收藏" name="favorite" />
        <el-tab-pane label="历史" name="history" />
      </el-tabs>

      <AppEmpty v-if="tab === 'recommend' && !loading && list.length === 0" description="暂无推荐数据。">
        <el-button type="primary" @click="load">刷新</el-button>
      </AppEmpty>
      <el-table v-if="tab === 'recommend'" :data="list" v-loading="loading">
        <el-table-column prop="title" label="名称" min-width="240" />
        <el-table-column prop="org" label="组织/备注" min-width="180" />
        <el-table-column v-if="isPerson" label="已投入" width="120">
          <template #default="{ row }">
            <el-tag v-if="deliveredCountForCompany(row.companyAccount)" type="success">{{ deliveredCountForCompany(row.companyAccount) }} 个版本</el-tag>
            <span v-else class="text-sm text-zinc-500">未投入</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="匹配度" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isPerson ? 280 : 200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="isPerson"
              link
              type="primary"
              :disabled="resumeVersions.length === 0 || !row.companyAccount"
              @click="openPoolDialog(row)"
            >
              投入/取出
            </el-button>
            <el-button link :type="favoriteSet.has(row.recordId) ? 'warning' : 'info'" @click="toggleFav(row.recordId)">
              {{ favoriteSet.has(row.recordId) ? '已收藏' : '收藏' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <AppEmpty v-else-if="tab === 'favorite' && favoriteList.length === 0" description="暂无收藏。">
        <el-button @click="tab = 'recommend'">去推荐列表</el-button>
      </AppEmpty>
      <el-table v-else-if="tab === 'favorite'" :data="favoriteList">
        <el-table-column prop="title" label="名称" min-width="240" />
        <el-table-column prop="org" label="组织/备注" min-width="180" />
        <el-table-column v-if="isPerson" label="已投入" width="120">
          <template #default="{ row }">
            <el-tag v-if="deliveredCountForCompany(row.companyAccount)" type="success">{{ deliveredCountForCompany(row.companyAccount) }} 个版本</el-tag>
            <span v-else class="text-sm text-zinc-500">未投入</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="匹配度" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isPerson ? 280 : 200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="isPerson"
              link
              type="primary"
              :disabled="resumeVersions.length === 0 || !row.companyAccount"
              @click="openPoolDialog(row)"
            >
              投入/取出
            </el-button>
            <el-button link type="danger" @click="toggleFav(row.recordId)">取消收藏</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AppEmpty v-else-if="tab === 'history' && historyList.length === 0" description="暂无历史记录。" />
      <el-table v-else :data="historyList">
        <el-table-column prop="title" label="名称" min-width="240" />
        <el-table-column prop="org" label="组织/备注" min-width="180" />
        <el-table-column v-if="isPerson" label="已投入" width="120">
          <template #default="{ row }">
            <el-tag v-if="deliveredCountForCompany(row.companyAccount)" type="success">{{ deliveredCountForCompany(row.companyAccount) }} 个版本</el-tag>
            <span v-else class="text-sm text-zinc-500">未投入</span>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="匹配度" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewedAt" label="最近查看" width="200">
          <template #default="{ row }">
            <span class="text-xs text-zinc-600">{{ fmt(row.viewedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isPerson ? 280 : 200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button
              v-if="isPerson"
              link
              type="primary"
              :disabled="resumeVersions.length === 0 || !row.companyAccount"
              @click="openPoolDialog(row)"
            >
              投入/取出
            </el-button>
            <el-button link :type="favoriteSet.has(row.recordId) ? 'warning' : 'info'" @click="toggleFav(row.recordId)">
              {{ favoriteSet.has(row.recordId) ? '已收藏' : '收藏' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <el-dialog v-model="poolDialog" title="投入/取出简历版本" width="560px">
    <div class="space-y-4">
      <div class="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-3 text-sm text-zinc-700">
        <div class="font-medium text-zinc-900">{{ activeRow?.title || '-' }}</div>
        <div class="mt-1 text-zinc-600">{{ activeRow?.org || '-' }}</div>
        <div class="mt-2 text-xs text-zinc-500">目标企业账号：{{ activeCompanyAccount || '未知' }}</div>
      </div>

      <AppEmpty v-if="resumeVersions.length === 0" description="文档中心暂无已完成的简历版本。">
        <el-button type="primary" @click="router.push('/person/doc/list')">去文档中心</el-button>
      </AppEmpty>

      <template v-else>
        <div>
          <div class="mb-2 text-sm font-medium text-zinc-800">选择投入版本</div>
          <el-select v-model="poolForm.docId" class="w-full" placeholder="请选择简历版本">
            <el-option
              v-for="doc in resumeVersions"
              :key="doc.id"
              :label="`${doc.fileName} · ${doc.createdAt}`"
              :value="doc.id"
            />
          </el-select>
        </div>

        <div class="rounded-lg border border-zinc-200 bg-white px-3 py-3 text-sm text-zinc-700">
          <div>当前选中：{{ selectedVersionLabel || '未选择' }}</div>
          <div class="mt-2">
            状态：
            <el-tag :type="selectedVersionDelivered ? 'success' : 'info'">
              {{ selectedVersionDelivered ? '已投入该企业' : '尚未投入该企业' }}
            </el-tag>
          </div>
        </div>

        <div>
          <div class="mb-2 text-sm font-medium text-zinc-800">当前已投入的版本</div>
          <div v-if="deliveredVersionsForActiveCompany.length" class="flex flex-wrap gap-2">
            <el-tag v-for="doc in deliveredVersionsForActiveCompany" :key="doc.id" type="success">
              {{ doc.fileName }}
            </el-tag>
          </div>
          <div v-else class="text-sm text-zinc-500">当前企业还没有收到你的任何简历版本。</div>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="poolDialog = false">关闭</el-button>
      <el-button
        type="danger"
        :disabled="resumeVersions.length === 0 || !poolForm.docId || !selectedVersionDelivered"
        :loading="poolLoading"
        @click="submitPoolChange(false)"
      >
        取出
      </el-button>
      <el-button
        type="primary"
        :disabled="resumeVersions.length === 0 || !poolForm.docId || selectedVersionDelivered"
        :loading="poolLoading"
        @click="submitPoolChange(true)"
      >
        投入
      </el-button>
    </template>
  </el-dialog>
</template>
