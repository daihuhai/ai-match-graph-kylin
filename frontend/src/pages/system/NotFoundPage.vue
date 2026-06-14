<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="mx-auto max-w-[560px] space-y-3 rounded-2xl border border-app-border bg-app-panel p-6">
    <div class="text-lg font-semibold text-app-text">404 页面不存在</div>
    <div class="text-sm text-app-text">请检查地址是否正确，或返回登录页重新进入。</div>
    <div class="flex justify-end gap-2">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" @click="router.push('/auth/login')">去登录</el-button>
    </div>
  </div>
</template>

