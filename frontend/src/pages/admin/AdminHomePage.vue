<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminUsersStore } from '@/stores/adminUsers'
import { useDocumentStore } from '@/stores/document'
import { useAuditStore } from '@/stores/audit'
import { useAdminDataStore } from '@/stores/adminData'
import { Users, FileText, Activity, Database, History, ShieldCheck, TrendingUp, AlertTriangle } from 'lucide-vue-next'

const router = useRouter()
const usersStore = useAdminUsersStore()
const docStore = useDocumentStore()
const auditStore = useAuditStore()
const adminData = useAdminDataStore()

onMounted(() => {
  docStore.hydrate()
  auditStore.hydrate()
  adminData.hydrate()
})

const totalDocs = computed(() => docStore.docs.length)
const docsDone = computed(() => docStore.docs.filter((d) => d.status === 'DONE').length)
const docsFailed = computed(() => docStore.docs.filter((d) => d.status === 'FAILED').length)
const totalAuditLogs = computed(() => auditStore.logs.length)
const recentAudit24h = computed(() => {
  const cutoff = Date.now() - 24 * 3600 * 1000
  return auditStore.logs.filter((x) => new Date(x.time).getTime() >= cutoff).length
})
const skillsCount = computed(() => adminData.skills.length)
const synonymsCount = computed(() => adminData.synonyms.length)

const summaryStats = computed(() => [
  { label: '文档总数', value: totalDocs.value, sub: `${docsDone.value} 完成 / ${docsFailed.value} 失败`, icon: FileText, color: 'purple' },
  { label: '审计日志', value: totalAuditLogs.value, sub: `24h 内 ${recentAudit24h.value} 条`, icon: ShieldCheck, color: 'emerald' },
  { label: '技能词条', value: skillsCount.value, sub: `${synonymsCount.value} 组同义词`, icon: Database, color: 'amber' },
])

const colorClasses: Record<string, { iconBg: string; text: string }> = {
  purple: { iconBg: 'bg-purple-50 dark:bg-purple-900/30', text: 'text-purple-600 dark:text-purple-400' },
  emerald: { iconBg: 'bg-emerald-50 dark:bg-emerald-900/30', text: 'text-emerald-600 dark:text-emerald-400' },
  amber: { iconBg: 'bg-amber-50 dark:bg-amber-900/30', text: 'text-amber-600 dark:text-amber-400' },
}
</script>

<template>
  <div class="space-y-6">
    <!-- Welcome Banner -->
    <div class="relative overflow-hidden rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-700 p-8 text-white shadow-lg">
      <div class="relative z-10 flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h2 class="text-2xl font-bold mb-2">管理端概览</h2>
          <p class="text-emerald-100 max-w-xl leading-relaxed">
            管理平台用户、文档库、技能数据与系统监控，实时掌握平台运营状态。
          </p>
        </div>
      </div>
      <div class="absolute right-0 top-0 -mt-12 -mr-12 w-64 h-64 rounded-full bg-app-panel opacity-10 blur-3xl"></div>
      <div class="absolute left-0 bottom-0 -mb-12 -ml-12 w-48 h-48 rounded-full bg-teal-400 opacity-20 blur-3xl"></div>
    </div>

    <!-- Summary Stats -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div v-for="s in summaryStats" :key="s.label" class="bg-app-panel dark:bg-zinc-900 rounded-xl border border-app-border dark:border-slate-800 p-5 flex items-center gap-4">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0" :class="[colorClasses[s.color]?.iconBg, colorClasses[s.color]?.text]">
          <component :is="s.icon" class="w-6 h-6" />
        </div>
        <div class="min-w-0">
          <div class="text-2xl font-bold text-app-text dark:text-white">{{ s.value }}</div>
          <div class="text-sm font-medium text-app-text dark:text-slate-300">{{ s.label }}</div>
          <div class="text-xs text-app-subtext dark:text-slate-400 truncate">{{ s.sub }}</div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-6 md:grid-cols-3">
      <el-card shadow="hover" class="!border-app-border dark:!border-slate-800 !rounded-xl cursor-pointer hover:!border-emerald-400 dark:hover:!border-emerald-500 transition-colors group" @click="router.push('/admin/users')">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-xl bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 group-hover:scale-110 transition-transform duration-300">
            <Users class="w-6 h-6" />
          </div>
          <div>
            <div class="text-base font-semibold text-app-text dark:text-white">用户管理</div>
            <div class="mt-1 text-sm text-app-subtext dark:text-slate-400 leading-relaxed">管理用户账号、权限与状态。</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="!border-app-border dark:!border-slate-800 !rounded-xl cursor-pointer hover:!border-emerald-400 dark:hover:!border-emerald-500 transition-colors group" @click="router.push('/admin/docs')">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-xl bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 group-hover:scale-110 transition-transform duration-300">
            <FileText class="w-6 h-6" />
          </div>
          <div>
            <div class="text-base font-semibold text-app-text dark:text-white">文档库</div>
            <div class="mt-1 text-sm text-app-subtext dark:text-slate-400 leading-relaxed">查看全平台上传记录与解析结果。</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="!border-app-border dark:!border-slate-800 !rounded-xl cursor-pointer hover:!border-emerald-400 dark:hover:!border-emerald-500 transition-colors group" @click="router.push('/admin/monitor')">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-xl bg-rose-50 dark:bg-rose-900/30 text-rose-600 dark:text-rose-400 group-hover:scale-110 transition-transform duration-300">
            <Activity class="w-6 h-6" />
          </div>
          <div>
            <div class="text-base font-semibold text-app-text dark:text-white">运营监控</div>
            <div class="mt-1 text-sm text-app-subtext dark:text-slate-400 leading-relaxed">服务状态、解析指标与系统大屏。</div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="grid grid-cols-1 gap-6 md:grid-cols-3">
      <el-card shadow="hover" class="!border-app-border dark:!border-slate-800 !rounded-xl cursor-pointer hover:!border-emerald-400 dark:hover:!border-emerald-500 transition-colors group" @click="router.push('/admin/data')">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-xl bg-amber-50 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 group-hover:scale-110 transition-transform duration-300">
            <Database class="w-6 h-6" />
          </div>
          <div>
            <div class="text-base font-semibold text-app-text dark:text-white">数据维护</div>
            <div class="mt-1 text-sm text-app-subtext dark:text-slate-400 leading-relaxed">技能库、同义词及基础字典配置。</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="!border-app-border dark:!border-slate-800 !rounded-xl cursor-pointer hover:!border-emerald-400 dark:hover:!border-emerald-500 transition-colors group" @click="router.push('/admin/match')">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 group-hover:scale-110 transition-transform duration-300">
            <History class="w-6 h-6" />
          </div>
          <div>
            <div class="text-base font-semibold text-app-text dark:text-white">匹配记录</div>
            <div class="mt-1 text-sm text-app-subtext dark:text-slate-400 leading-relaxed">查看全站匹配历史与用户反馈。</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="!border-app-border dark:!border-slate-800 !rounded-xl cursor-pointer hover:!border-emerald-400 dark:hover:!border-emerald-500 transition-colors group" @click="router.push('/admin/audit')">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-xl bg-app-bg dark:bg-slate-800 text-app-text dark:text-slate-300 group-hover:scale-110 transition-transform duration-300">
            <ShieldCheck class="w-6 h-6" />
          </div>
          <div>
            <div class="text-base font-semibold text-app-text dark:text-white">日志审计</div>
            <div class="mt-1 text-sm text-app-subtext dark:text-slate-400 leading-relaxed">操作追踪、安全合规与详细记录。</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>
