<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore, type UserType } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const form = reactive<{ account: string; password: string; userType: UserType }>({
  account: 'demo',
  password: '123456',
  userType: 'PERSON',
})

const redirect = () => {
  const r = route.query.redirect
  if (typeof r === 'string' && r.startsWith('/')) return r
  if (auth.userType === 'ADMIN') return '/admin'
  if (auth.userType === 'COMPANY') return '/company/dashboard'
  return '/person/dashboard'
}

const submit = async () => {
  loading.value = true
  try {
    const resp = await login(form)
    auth.setAuth(resp)
    await router.replace(redirect())
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <div>
      <div class="text-lg font-semibold text-zinc-900">登录</div>
      <div class="mt-1 text-sm text-zinc-500">无后端时默认走 mock；设置 VITE_USE_MOCK=false 可切到真实接口</div>
    </div>

    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="账号">
        <el-input v-model="form.account" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.userType" class="w-full">
          <el-option label="个人用户" value="PERSON" />
          <el-option label="企业用户" value="COMPANY" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-button type="primary" class="w-full" :loading="loading" @click="submit">登录</el-button>
    </el-form>

    <div class="text-sm text-zinc-600">
      没有账号？
      <router-link class="text-blue-600 hover:underline" to="/auth/register">去注册</router-link>
    </div>
  </div>
</template>
