<script setup lang="ts">
defineProps<{
  description?: string
}>()
</script>

<template>
  <div class="py-10">
    <el-empty :description="description || '暂无数据'">
      <slot />
    </el-empty>
  </div>
</template>

