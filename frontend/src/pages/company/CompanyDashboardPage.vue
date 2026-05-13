<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="text-base font-semibold">企业工作台</div>
          <div class="mt-1 text-sm text-zinc-600">
            上传 JD 后可进入人才市场与人才库推荐流程，并通过消息中心与感兴趣的个人用户继续沟通。
          </div>
        </div>
        <el-button type="primary" @click="router.push('/company/doc/list')">进入文档中心</el-button>
      </div>
    </el-card>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/company/graph/job-001')">
        <div class="text-sm font-semibold">职位图谱</div>
        <div class="mt-1 text-sm text-zinc-600">查看岗位技能要求网络与关联关系。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/company/match/candidates')">
        <div class="text-sm font-semibold">人才库推荐</div>
        <div class="mt-1 text-sm text-zinc-600">查看推荐候选人、差距项与建议。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/company/messages')">
        <div class="text-sm font-semibold">消息中心</div>
        <div class="mt-1 text-sm text-zinc-600">查看与个人用户的沟通记录，继续聊一聊。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/company/doc/list')">
        <div class="text-sm font-semibold">文档中心</div>
        <div class="mt-1 text-sm text-zinc-600">查看上传记录、解析任务与结果入口。</div>
      </el-card>
    </div>
  </div>
</template>
