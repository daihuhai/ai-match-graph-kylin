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
  } catch (e: any) {
    ElMessage.error(e?.message || '注册失败')
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
          to="/auth/register/person"
          class="flex-1 rounded-md px-3 py-2 text-center text-sm font-medium transition"
          :class="route.name === 'RegisterPerson' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500'"
        >
          个人注册
        </router-link>
        <router-link
          to="/auth/register/company"
          class="flex-1 rounded-md px-3 py-2 text-center text-sm font-medium transition"
          :class="route.name === 'RegisterCompany' ? 'bg-white text-zinc-900 shadow-sm' : 'text-zinc-500'"
        >
          企业注册
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
        <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
      </el-form-item>
      <el-button type="primary" class="w-full" :loading="loading" @click="submit">{{ roleCopy.submitLabel }}</el-button>
    </el-form>

    <div class="text-sm text-zinc-600">
      已有账号？
      <router-link class="text-blue-600 hover:underline" :to="roleCopy.loginRoute">去登录</router-link>
    </div>
  </div>
</template>
