<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">个人工作台</div>
          <div class="mt-1 text-sm text-zinc-600">从上传简历开始，完成解析、图谱与推荐查看。</div>
        </div>
        <el-button type="primary" @click="router.push('/person/doc/list')">进入文档中心</el-button>
      </div>
    </el-card>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/graph/person-001')">
        <div class="text-sm font-semibold">能力图谱</div>
        <div class="mt-1 text-sm text-zinc-600">查看个人技能网络与关联证据。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/match/jobs')">
        <div class="text-sm font-semibold">职位推荐</div>
        <div class="mt-1 text-sm text-zinc-600">查看 Top-N 推荐与可解释匹配详情。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/doc/list')">
        <div class="text-sm font-semibold">文档中心</div>
        <div class="mt-1 text-sm text-zinc-600">查看上传记录、解析任务与结果入口。</div>
      </el-card>
    </div>
  </div>
</template>
