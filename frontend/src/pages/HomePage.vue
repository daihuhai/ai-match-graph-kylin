<script setup lang="ts">
import { onBeforeMount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

onBeforeMount(() => {
  auth.hydrate()
  if (auth.isLoggedIn) {
    switch (auth.userType) {
      case 'PERSON': router.replace('/person/dashboard'); break
      case 'COMPANY': router.replace('/company/dashboard'); break
      case 'ADMIN': router.replace('/admin'); break
      default: router.replace({ name: 'Login' })
    }
  } else {
    router.replace({ name: 'Login' })
  }
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-app-bg">
    <div class="text-center">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-600 mb-4">
        <svg class="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
      </div>
      <h1 class="text-xl font-bold text-app-text">AI 智能匹配与能力图谱系统</h1>
      <p class="text-sm text-app-subtext mt-2">正在跳转…</p>
    </div>
  </div>
</template>
