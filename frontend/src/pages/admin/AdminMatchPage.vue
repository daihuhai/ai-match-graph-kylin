<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getDataStorage } from '@/utils/storage'
import type { MatchFeedback, MatchHistoryItem } from '@/types/match'
import { downloadCsv, downloadJson } from '@/utils/export'
import { maskUserKey } from '@/utils/mask'

const MATCH_KEY = 'aimap.match'

const query = reactive<{ userKey: string; recordId: string }>({ userKey: '', recordId: '' })
const tab = ref<'history' | 'feedback'>('history')

const state = ref<{ history: MatchHistoryItem[]; feedbacks: MatchFeedback[] }>({ history: [], feedbacks: [] })

const load = () => {
  const raw = getDataStorage().getItem(MATCH_KEY)
  if (!raw) {
    state.value = { history: [], feedbacks: [] }
    return
  }
  try {
    const obj = JSON.parse(raw) as any
    state.value = {
      history: Array.isArray(obj.history) ? (obj.history as MatchHistoryItem[]) : [],
      feedbacks: Array.isArray(obj.feedbacks) ? (obj.feedbacks as MatchFeedback[]) : [],
    }
  } catch {
    state.value = { history: [], feedbacks: [] }
  }
}

onMounted(load)

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

const filteredHistory = computed(() => {
  const u = query.userKey.trim()
  const r = query.recordId.trim()
  return state.value.history.filter((x) => (!u || x.userKey.includes(u)) && (!r || x.recordId.includes(r)))
})

const filteredFeedback = computed(() => {
  const u = query.userKey.trim()
  const r = query.recordId.trim()
  return state.value.feedbacks.filter((x) => (!u || x.userKey.includes(u)) && (!r || x.recordId.includes(r)))
})

const exportJson = () => {
  downloadJson(`admin-match-${Date.now()}.json`, tab.value === 'history' ? filteredHistory.value : filteredFeedback.value)
}

const exportCsv = () => {
  if (tab.value === 'history') {
    downloadCsv(
      `admin-match-history-${Date.now()}.csv`,
      filteredHistory.value.map((r) => ({
        viewedAt: r.viewedAt,
        userKey: maskUserKey(r.userKey),
        side: r.side,
        recordId: r.recordId,
        title: r.title,
        org: r.org,
        score: r.score,
      })),
    )
  } else {
    downloadCsv(
      `admin-match-feedback-${Date.now()}.csv`,
      filteredFeedback.value.map((r) => ({
        createdAt: r.createdAt,
        userKey: maskUserKey(r.userKey),
        recordId: r.recordId,
        rating: r.rating,
        tags: r.tags.join('|'),
        comment: r.comment,
      })),
    )
  }
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div class="text-base font-semibold">匹配记录</div>
          <div class="mt-1 text-sm text-zinc-600">聚合查看用户端的匹配历史与反馈（读取本地存储）。</div>
        </div>
        <el-button v-permission="'ADMIN_MATCH_VIEW'" @click="load">刷新</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
        <el-input v-model="query.userKey" placeholder="用户Key（如 PERSON:demo）" clearable />
        <el-input v-model="query.recordId" placeholder="RecordId" clearable />
        <div class="flex items-center justify-end">
          <el-button v-permission="'ADMIN_MATCH_VIEW'" @click="exportJson">导出 JSON</el-button>
          <el-button v-permission="'ADMIN_MATCH_VIEW'" type="primary" @click="exportCsv">导出 CSV</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="历史" name="history" />
        <el-tab-pane label="反馈" name="feedback" />
      </el-tabs>

      <el-table v-if="tab === 'history'" :data="filteredHistory">
        <el-table-column prop="viewedAt" label="时间" width="200">
          <template #default="{ row }">{{ fmt(row.viewedAt) }}</template>
        </el-table-column>
        <el-table-column prop="userKey" label="用户" width="200">
          <template #default="{ row }">{{ maskUserKey(row.userKey) }}</template>
        </el-table-column>
        <el-table-column prop="side" label="端" width="100" />
        <el-table-column prop="recordId" label="RecordId" width="180" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="org" label="备注" min-width="160" />
        <el-table-column prop="score" label="分数" width="100" />
      </el-table>

      <el-table v-else :data="filteredFeedback">
        <el-table-column prop="createdAt" label="时间" width="200">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="userKey" label="用户" width="200">
          <template #default="{ row }">{{ maskUserKey(row.userKey) }}</template>
        </el-table-column>
        <el-table-column prop="recordId" label="RecordId" width="180" />
        <el-table-column prop="rating" label="评分" width="120" />
        <el-table-column label="标签" width="220">
          <template #default="{ row }">
            <div class="flex flex-wrap gap-2">
              <el-tag v-for="t in row.tags" :key="t" type="info">{{ t }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="说明" min-width="260" />
      </el-table>
    </el-card>
  </div>
</template>
