<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { recommendCandidates, recommendJobs } from '@/api/match'
import type { MatchListItem } from '@/types/match'
import { useMatchStore } from '@/stores/match'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import AppEmpty from '@/components/AppEmpty.vue'

const route = useRoute()
const router = useRouter()
const matchStore = useMatchStore()
const auth = useAuthStore()
const audit = useAuditStore()

const isCompany = computed(() => route.path.startsWith('/company'))
const title = computed(() => (isCompany.value ? '人才库推荐' : '人才市场 · 职位推荐'))
const list = ref<MatchListItem[]>([])
const loading = ref(true)
const tab = ref<'recommend' | 'favorite' | 'history'>('recommend')
const minMatch = ref(0)

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

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
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
            霍兰德 RIASEC：企业端从系统人才库匹配候选人；个人端从人才市场（含企业发布的 JD）匹配岗位。可设置最低匹配度（后端过滤）。
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
      <el-table
        v-if="tab === 'recommend'"
        :data="list"
        v-loading="loading"
      >
        <el-table-column prop="title" label="名称" min-width="240" />
        <el-table-column prop="org" label="组织/备注" min-width="180" />
        <el-table-column prop="score" label="匹配度" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button link :type="favoriteSet.has(row.recordId) ? 'warning' : 'info'" @click="toggleFav(row.recordId)">
              {{ favoriteSet.has(row.recordId) ? '已收藏' : '收藏' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <AppEmpty v-else-if="tab === 'favorite' && favoriteList.length === 0" description="暂无收藏。">
        <el-button @click="tab = 'recommend'">去推荐列表</el-button>
      </AppEmpty>
      <el-table
        v-else-if="tab === 'favorite'"
        :data="favoriteList"
      >
        <el-table-column prop="title" label="名称" min-width="240" />
        <el-table-column prop="org" label="组织/备注" min-width="180" />
        <el-table-column prop="score" label="匹配度" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button link type="danger" @click="toggleFav(row.recordId)">取消收藏</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AppEmpty v-else-if="tab === 'history' && historyList.length === 0" description="暂无历史记录。" />
      <el-table
        v-else
        :data="historyList"
      >
        <el-table-column prop="title" label="名称" min-width="240" />
        <el-table-column prop="org" label="组织/备注" min-width="180" />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <el-button link :type="favoriteSet.has(row.recordId) ? 'warning' : 'info'" @click="toggleFav(row.recordId)">
              {{ favoriteSet.has(row.recordId) ? '已收藏' : '收藏' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
