<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore, type UserType } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)

const form = reactive<{ account: string; password: string }>({
  account: 'demo',
  password: '123456',
})

const role = computed<UserType>(() => {
  if (route.name === 'LoginCompany') return 'COMPANY'
  if (route.name === 'LoginAdmin') return 'ADMIN'
  return 'PERSON'
})

const roleCopy = computed(() => {
  if (role.value === 'COMPANY') {
    return {
      title: '企业登录',
      description: '登录后进入企业端工作台、文档中心和人才库推荐。',
      registerRoute: '/auth/register/company',
      registerLabel: '去企业注册',
    }
  }
  if (role.value === 'ADMIN') {
    return {
      title: '管理员登录',
      description: '仅用于平台运营、用户管理和审计查看。',
      registerRoute: '',
      registerLabel: '',
    }
  }
  return {
    title: '个人登录',
    description: '登录后进入个人端工作台、简历上传和岗位推荐。',
    registerRoute: '/auth/register/person',
    registerLabel: '去个人注册',
  }
})

watch(
  () => route.name,
  (name) => {
    form.account = name === 'LoginAdmin' ? 'admin' : 'demo'
    form.password = '123456'
  },
  { immediate: true },
)

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
    const resp = await login({
      account: form.account,
      password: form.password,
      userType: role.value,
    })
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
  <div class="space-y-5">
    <div class="space-y-3">
      <div class="inline-flex w-full rounded-lg bg-zinc-100 p-1">
        <router-link
          to="/auth/login/person"
          class="flex-1 rounded-md px-3 py-2 text-center text-sm font-medium transition"
          :class="route.name === 'LoginPerson' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500'"
        >
          个人登录
        </router-link>
        <router-link
          to="/auth/login/company"
          class="flex-1 rounded-md px-3 py-2 text-center text-sm font-medium transition"
          :class="route.name === 'LoginCompany' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500'"
        >
          企业登录
        </router-link>
        <router-link
          to="/auth/login/admin"
          class="flex-1 rounded-md px-3 py-2 text-center text-sm font-medium transition"
          :class="route.name === 'LoginAdmin' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500'"
        >
          管理员
        </router-link>
      </div>

      <div>
        <div class="text-lg font-semibold text-zinc-900">{{ roleCopy.title }}</div>
        <div class="mt-1 text-sm leading-6 text-zinc-500">
          {{ roleCopy.description }}
        </div>
      </div>
    </div>

    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名 / 手机号 / 邮箱">
        <el-input v-model="form.account" autocomplete="username" placeholder="与注册时填写的信息保持一致" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
      </el-form-item>
      <el-button type="primary" class="w-full" :loading="loading" @click="submit">登录</el-button>
    </el-form>

    <div v-if="roleCopy.registerRoute" class="text-sm text-zinc-600">
      还没有账号？
      <router-link class="text-blue-600 hover:underline" :to="roleCopy.registerRoute">{{ roleCopy.registerLabel }}</router-link>
    </div>
  </div>
</template>
