<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore, type UserType } from '@/stores/auth'
import { User, Lock } from 'lucide-vue-next'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)

const form = reactive<{ account: string; password: string }>({
  account: '',
  password: '',
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
      description: '登录企业账户，发现优质人才',
      registerRoute: '/auth/register/company',
      registerLabel: '注册企业账号',
    }
  }
  if (role.value === 'ADMIN') {
    return {
      title: '管理员登录',
      description: '系统管理与运营监控',
      registerRoute: '',
      registerLabel: '',
    }
  }
  return {
    title: '个人登录',
    description: '连接您的职业档案，发现匹配机会',
    registerRoute: '/auth/register/person',
    registerLabel: '注册个人账号',
  }
})

watch(
  () => route.name,
  () => {
    form.account = ''
    form.password = ''
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
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-8 w-full">
    <!-- Role Selector -->
    <div class="flex p-1 bg-app-bg border border-app-border rounded-xl relative">
      <router-link
        to="/auth/login/person"
        class="flex-1 py-2 text-center text-sm font-medium rounded-lg transition-all duration-300 z-10"
        :class="route.name === 'LoginPerson' ? 'text-indigo-600 font-semibold' : 'text-app-subtext hover:text-app-text'"
      >
        个人
      </router-link>
      <router-link
        to="/auth/login/company"
        class="flex-1 py-2 text-center text-sm font-medium rounded-lg transition-all duration-300 z-10"
        :class="route.name === 'LoginCompany' ? 'text-indigo-600 font-semibold' : 'text-app-subtext hover:text-app-text'"
      >
        企业
      </router-link>
      <router-link
        to="/auth/login/admin"
        class="flex-1 py-2 text-center text-sm font-medium rounded-lg transition-all duration-300 z-10"
        :class="route.name === 'LoginAdmin' ? 'text-indigo-600 font-semibold' : 'text-app-subtext hover:text-app-text'"
      >
        系统管理
      </router-link>
      <!-- Active Pill indicator -->
      <div 
        class="absolute top-1 bottom-1 bg-app-panel"
        :style="{
          width: 'calc(33.333% - 4px)',
          left: route.name === 'LoginPerson' ? '4px' : route.name === 'LoginCompany' ? 'calc(33.333% + 2px)' : 'calc(66.666%)'
        }"
      ></div>
    </div>

    <!-- Header -->
    <div>
      <h2 class="text-3xl font-display font-bold text-app-text mb-2">{{ roleCopy.title }}</h2>
      <p class="text-app-subtext text-sm">{{ roleCopy.description }}</p>
    </div>

    <!-- Form -->
    <form @submit.prevent="submit" class="space-y-6">
      <div class="space-y-1">
        <label class="block text-sm font-medium text-app-text mb-1.5">账号</label>
        <div class="relative group">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-app-subtext group-focus-within:text-indigo-500 transition-colors">
            <User class="w-5 h-5" />
          </div>
          <input
            v-model="form.account"
            type="text"
            name="account"
            autocomplete="username"
            class="w-full h-11 bg-app-panel border border-app-border rounded-lg pl-10 pr-4 text-app-text placeholder:text-app-subtext focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-all"
            placeholder="请输入账号"
          />
        </div>
      </div>

      <div class="space-y-1">
        <label class="block text-sm font-medium text-app-text mb-1.5">密码</label>
        <div class="relative group">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-app-subtext group-focus-within:text-indigo-500 transition-colors">
            <Lock class="w-5 h-5" />
          </div>
          <input
            v-model="form.password"
            type="password"
            name="password"
            autocomplete="current-password"
            class="w-full h-11 bg-app-panel border border-app-border rounded-lg pl-10 pr-4 text-app-text placeholder:text-app-subtext focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 transition-all"
            placeholder="请输入密码"
          />
        </div>
      </div>

      <button 
        type="submit" 
        :disabled="loading"
        class="w-full rounded-lg bg-indigo-600 text-white font-semibold py-3 px-4 mt-4 hover:bg-indigo-700 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-sm"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>

    <!-- Register Link -->
    <div v-if="roleCopy.registerRoute" class="text-center pt-2">
      <span class="text-app-subtext text-sm">还没有账号?</span>
      <router-link :to="roleCopy.registerRoute" class="ml-2 text-indigo-600 hover:text-indigo-800 hover:underline transition-colors text-sm font-semibold">
        {{ roleCopy.registerLabel }}
      </router-link>
    </div>
  </div>
</template>
