<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UserType } from '@/stores/auth'
import { register } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const role = computed<UserType>(() => (route.name === 'RegisterCompany' ? 'COMPANY' : 'PERSON'))

const roleCopy = computed(() =>
  role.value === 'COMPANY'
    ? {
        title: '企业注册',
        description: '创建企业账号后，将进入企业端工作台、JD 上传与候选人推荐流程。',
        accountPlaceholder: '企业名称或企业登录账号',
        submitLabel: '注册企业账号',
        loginRoute: '/auth/login/company',
      }
    : {
        title: '个人注册',
        description: '创建个人账号后，将进入个人端工作台、简历上传与岗位推荐流程。',
        accountPlaceholder: '用户名或个人登录账号',
        submitLabel: '注册个人账号',
        loginRoute: '/auth/login/person',
      },
)

const form = reactive<{ account: string; password: string; phone: string; email: string }>({
  account: '',
  password: '',
  phone: '',
  email: '',
})

watch(
  () => route.name,
  () => {
    form.account = ''
    form.password = ''
    form.phone = ''
    form.email = ''
  },
)

const submit = async () => {
  loading.value = true
  try {
    await register({
      account: form.account.trim(),
      password: form.password,
      userType: role.value,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
    })
    ElMessage.success('注册成功')
    await router.replace(roleCopy.value.loginRoute)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '注册失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-5">
    <div class="space-y-3">
      <div class="inline-flex w-full rounded-xl bg-app-bg border border-app-border p-1.5">
        <router-link
          to="/auth/register/person"
          class="flex-1 rounded-lg px-3 py-2 text-center text-sm font-medium transition-all duration-200"
          :class="route.name === 'RegisterPerson' ? 'bg-app-panel text-indigo-600 shadow-sm border border-app-border' : 'text-app-subtext hover:text-app-text'"
        >
          个人注册
        </router-link>
        <router-link
          to="/auth/register/company"
          class="flex-1 rounded-lg px-3 py-2 text-center text-sm font-medium transition-all duration-200"
          :class="route.name === 'RegisterCompany' ? 'bg-app-panel text-indigo-600 shadow-sm border border-app-border' : 'text-app-subtext hover:text-app-text'"
        >
          企业注册
        </router-link>
      </div>

      <div class="pt-4 pb-2">
        <div class="text-2xl font-bold text-app-text">{{ roleCopy.title }}</div>
        <div class="mt-2 text-sm leading-6 text-app-subtext">
          {{ roleCopy.description }}
        </div>
      </div>
    </div>

    <el-form label-position="top" @submit.prevent="submit" size="large">
      <el-form-item label="用户名（登录账号）">
        <el-input v-model="form.account" autocomplete="username" :placeholder="roleCopy.accountPlaceholder" />
      </el-form-item>
      <el-form-item label="手机号（可选）">
        <el-input v-model="form.phone" autocomplete="tel" placeholder="填写后也可使用手机号登录" />
      </el-form-item>
      <el-form-item label="邮箱（可选）">
        <el-input v-model="form.email" autocomplete="email" placeholder="填写后也可使用邮箱登录" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" autocomplete="new-password" show-password @keyup.enter="submit" />
      </el-form-item>
      <button type="submit" class="w-full rounded-lg bg-indigo-600 text-white font-semibold py-3 px-4 mt-2 hover:bg-indigo-700 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm" :disabled="loading">
        {{ loading ? '注册中...' : roleCopy.submitLabel }}
      </button>
    </el-form>

    <div class="text-sm text-app-subtext text-center mt-6">
      已有账号？
      <router-link class="text-indigo-600 font-medium hover:text-indigo-800 hover:underline transition-colors" :to="roleCopy.loginRoute">去登录</router-link>
    </div>
  </div>
</template>
