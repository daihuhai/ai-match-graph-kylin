<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useAuditStore, type AuditLogRow } from '@/stores/audit'
import { downloadCsv, downloadJson } from '@/utils/export'
import { maskUserKey } from '@/utils/mask'

const store = useAuditStore()

onMounted(() => {
  store.hydrate()
})

const query = reactive({ user: '', module: '', result: '' as '' | 'OK' | 'FAIL' })

const list = computed(() => {
  const u = query.user.trim()
  const m = query.module.trim()
  return store.logs.filter((r) => {
    if (u && !r.userKey.includes(u)) return false
    if (m && !r.module.includes(m)) return false
    if (query.result && r.result !== query.result) return false
    return true
  })
})

const drawerOpen = ref(false)
const current = ref<AuditLogRow | null>(null)

const openDetail = (row: AuditLogRow) => {
  current.value = row
  drawerOpen.value = true
}

const fmt = (iso: string) => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

const exportJson = () => {
  downloadJson(`audit-${Date.now()}.json`, list.value)
}

const exportCsv = () => {
  downloadCsv(
    `audit-${Date.now()}.csv`,
    list.value.map((r) => ({
      time: r.time,
      userKey: maskUserKey(r.userKey),
      module: r.module,
      result: r.result,
      detail: JSON.stringify(r.detail),
    })),
  )
}

const clearAll = async () => {
  await ElMessageBox.confirm('确认清空所有审计日志（本地）？', '提示', { type: 'warning' })
  store.clear()
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">日志审计</div>
          <div class="mt-1 text-sm text-zinc-600">演示“按用户/模块筛选 + 表格展示”。</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-4">
        <el-input v-model="query.user" placeholder="用户Key（如 PERSON:demo）" clearable />
        <el-input v-model="query.module" placeholder="模块（如 match.）" clearable />
        <el-select v-model="query.result" placeholder="结果" clearable>
          <el-option label="OK" value="OK" />
          <el-option label="FAIL" value="FAIL" />
        </el-select>
        <div class="flex items-center justify-end">
          <el-button v-permission="'ADMIN_AUDIT_VIEW'" @click="exportJson">导出 JSON</el-button>
          <el-button v-permission="'ADMIN_AUDIT_VIEW'" @click="exportCsv">导出 CSV</el-button>
          <el-button v-permission="'ADMIN_AUDIT_VIEW'" type="danger" @click="clearAll">清空</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list">
        <el-table-column prop="time" label="时间" width="200">
          <template #default="{ row }">
            <span class="text-xs text-zinc-600">{{ fmt(row.time) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userKey" label="用户" width="200">
          <template #default="{ row }">
            <span class="text-xs text-zinc-700">{{ maskUserKey(row.userKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" min-width="240" />
        <el-table-column prop="result" label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.result === 'OK' ? 'success' : 'danger'">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'ADMIN_AUDIT_VIEW'" link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <el-drawer v-model="drawerOpen" title="审计详情" size="520px">
    <div v-if="!current" class="text-sm text-zinc-600">暂无</div>
    <div v-else class="space-y-3">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="时间">{{ fmt(current.time) }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ maskUserKey(current.userKey) }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ current.result }}</el-descriptions-item>
        <el-descriptions-item label="ID">{{ current.id }}</el-descriptions-item>
      </el-descriptions>
      <pre class="max-h-[420px] overflow-auto rounded-lg bg-zinc-950 p-4 text-xs text-zinc-100">{{
        JSON.stringify(current.detail, null, 2)
      }}</pre>
    </div>
  </el-drawer>
</template>
