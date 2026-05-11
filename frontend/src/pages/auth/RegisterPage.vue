<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UserType } from '@/stores/auth'
import { register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)

const form = reactive<{ account: string; password: string; userType: UserType }>({
  account: '',
  password: '',
  userType: 'PERSON',
})

const submit = async () => {
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功')
    await router.replace('/auth/login')
  } catch (e: any) {
    ElMessage.error(e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <div>
      <div class="text-lg font-semibold text-zinc-900">注册</div>
      <div class="mt-1 text-sm text-zinc-500">默认走 mock；设置 VITE_USE_MOCK=false 可切到真实接口</div>
    </div>

    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="角色">
        <el-select v-model="form.userType" class="w-full">
          <el-option label="个人用户" value="PERSON" />
          <el-option label="企业用户" value="COMPANY" />
        </el-select>
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="form.account" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
      </el-form-item>
      <el-button type="primary" class="w-full" :loading="loading" @click="submit">注册</el-button>
    </el-form>

    <div class="text-sm text-zinc-600">
      已有账号？
      <router-link class="text-blue-600 hover:underline" to="/auth/login">去登录</router-link>
    </div>
  </div>
</template>

