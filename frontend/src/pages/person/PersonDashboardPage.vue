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
          <div class="mt-1 text-sm text-zinc-600">
            上传简历解析后，可查看与人才市场岗位的匹配结果；若主动投递给企业，也可以直接在消息中心继续沟通。
          </div>
        </div>
        <el-button type="primary" @click="router.push('/person/doc/list')">进入文档中心</el-button>
      </div>
    </el-card>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/graph/person-001')">
        <div class="text-sm font-semibold">能力图谱</div>
        <div class="mt-1 text-sm text-zinc-600">查看个人技能网络与关联证据。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/match/jobs')">
        <div class="text-sm font-semibold">人才市场</div>
        <div class="mt-1 text-sm text-zinc-600">浏览岗位推荐与匹配详情。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/messages')">
        <div class="text-sm font-semibold">消息中心</div>
        <div class="mt-1 text-sm text-zinc-600">查看与企业HR的沟通记录，继续聊一聊。</div>
      </el-card>
      <el-card shadow="never" class="cursor-pointer" @click="router.push('/person/doc/list')">
        <div class="text-sm font-semibold">文档中心</div>
        <div class="mt-1 text-sm text-zinc-600">查看上传记录、解析任务与结果入口。</div>
      </el-card>
    </div>
  </div>
</template>
