<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdminUsersStore, type AdminUserRow, type AdminUserStatus, type AdminUserType } from '@/stores/adminUsers'
import { downloadCsv, downloadJson } from '@/utils/export'

const store = useAdminUsersStore()

onMounted(async () => {
  try {
    await store.fetchUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载用户列表失败')
  }
})

const query = reactive<{ account: string; userType: AdminUserType | ''; status: AdminUserStatus | '' }>({
  account: '',
  userType: '',
  status: '',
})

const list = computed(() => {
  const account = query.account.trim().toLowerCase()
  return store.users.filter((u) => {
    if (account && !u.account.toLowerCase().includes(account)) return false
    if (query.userType && u.userType !== query.userType) return false
    if (query.status && u.status !== query.status) return false
    return true
  })
})

const dlgOpen = ref(false)
const form = reactive<{
  id?: number
  account: string
  userType: AdminUserType
  status: AdminUserStatus
  phone: string
  email: string
}>({
  id: undefined,
  account: '',
  userType: 'PERSON',
  status: 'ACTIVE',
  phone: '',
  email: '',
})

const resetForm = () => {
  form.id = undefined
  form.account = ''
  form.userType = 'PERSON'
  form.status = 'ACTIVE'
  form.phone = ''
  form.email = ''
}

const openCreate = () => {
  resetForm()
  dlgOpen.value = true
}

const openEdit = (row: AdminUserRow) => {
  form.id = row.id
  form.account = row.account
  form.userType = row.userType
  form.status = row.status
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  dlgOpen.value = true
}

const save = async () => {
  if (!form.account.trim()) return ElMessage.warning('请填写账号')
  try {
    await store.upsert({
      id: form.id,
      account: form.account.trim(),
      userType: form.userType,
      status: form.status,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
    })
    ElMessage.success('保存成功')
    dlgOpen.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const toggleStatus = async (row: AdminUserRow) => {
  const next: AdminUserStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(`确认将 ${row.account} 设置为${next === 'ACTIVE' ? '启用' : '禁用'}吗？`, '提示', { type: 'warning' })
  try {
    await store.setStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '已启用' : '已禁用')
  } catch (e: any) {
    ElMessage.error(e?.message || '状态更新失败')
  }
}

const remove = async (row: AdminUserRow) => {
  await ElMessageBox.confirm(`确认删除用户 ${row.account} 吗？`, '提示', { type: 'warning' })
  try {
    await store.remove(row.id)
    ElMessage.success('删除成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const resetPassword = async (row: AdminUserRow) => {
  await ElMessageBox.confirm(`确认重置 ${row.account} 的密码吗？`, '提示', { type: 'warning' })
  try {
    const resp = await store.resetPassword(row.id)
    ElMessage.success(`已重置为默认密码：${resp.defaultPassword}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '重置密码失败')
  }
}

const exportJson = () => {
  downloadJson(`admin-users-${Date.now()}.json`, list.value)
}

const exportCsv = () => {
  downloadCsv(
    `admin-users-${Date.now()}.csv`,
    list.value.map((u) => ({
      id: u.id,
      account: u.account,
      userType: u.userType,
      status: u.status,
      phone: u.phone ?? '',
      email: u.email ?? '',
      createdAt: u.createdAt ?? '',
      lastLoginAt: u.lastLoginAt ?? '',
    })),
  )
}

const fmt = (iso?: string) => {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">用户管理</div>
          <div class="mt-1 text-sm text-app-subtext">当前列表直接读取数据库用户表，支持新增、编辑、启用、禁用、重置密码与删除。</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="flex items-center gap-3">
        <el-input v-model="query.account" placeholder="账号" clearable class="w-[200px]" />
        <el-select v-model="query.userType" placeholder="角色" clearable class="w-[160px]">
          <el-option label="个人" value="PERSON" />
          <el-option label="企业" value="COMPANY" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable class="w-[140px]">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <div class="flex items-center gap-2 ml-auto">
          <el-button v-permission="'ADMIN_USERS_VIEW'" type="primary" @click="openCreate">新增用户</el-button>
          <el-button v-permission="'ADMIN_USERS_VIEW'" @click="exportJson">导出 JSON</el-button>
          <el-button v-permission="'ADMIN_USERS_VIEW'" @click="exportCsv">导出 CSV</el-button>
        </div>
      </div>

      <div class="mt-4">
        <el-table :data="list" v-loading="store.loading">
          <el-table-column prop="id" label="用户ID" width="100" />
          <el-table-column prop="account" label="账号" min-width="180" />
          <el-table-column prop="userType" label="角色" width="120" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="160" />
          <el-table-column prop="email" label="邮箱" min-width="220" />
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              <span class="text-xs text-app-subtext">{{ fmt(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginAt" label="最近登录" width="180">
            <template #default="{ row }">
              <span class="text-xs text-app-subtext">{{ fmt(row.lastLoginAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button v-permission="'ADMIN_USERS_VIEW'" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-permission="'ADMIN_USERS_VIEW'" link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button v-permission="'ADMIN_USERS_VIEW'" link type="info" @click="resetPassword(row)">重置密码</el-button>
              <el-button v-permission="'ADMIN_USERS_VIEW'" link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="dlgOpen" title="用户" width="520px">
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.account" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.userType" class="w-full">
            <el-option label="个人" value="PERSON" />
            <el-option label="企业" value="COMPANY" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="w-full">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgOpen = false">取消</el-button>
        <el-button v-permission="'ADMIN_USERS_VIEW'" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
