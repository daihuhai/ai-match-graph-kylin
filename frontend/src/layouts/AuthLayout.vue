<template>
  <div ref="containerRef" class="min-h-screen w-full flex items-center justify-center overflow-hidden relative">

    <!-- Background Image -->
    <div class="absolute inset-0 z-0">
      <img src="/login-bg.png" alt="" width="1920" height="1080" class="w-full h-full object-cover" />
    </div>

    <!-- Subtle overlay for readability -->
    <div class="absolute inset-0 z-[1] bg-black/[0.03]"></div>

    <!-- Form Container - Centered -->
    <div class="relative z-10 w-full max-w-[420px] mx-auto px-4">
      <!-- Branding -->
      <div ref="brandingRef" class="text-center mb-8">
        <h1 class="text-2xl font-display font-bold text-app-text mb-2">AI 智能匹配与能力图谱系统</h1>
        <p class="text-app-subtext text-sm">驱动人才与岗位的高效双向匹配</p>
      </div>

      <!-- Form Card -->
      <div ref="cardRef" class="bg-app-panel/95 backdrop-blur-md rounded-2xl p-8 shadow-xl border border-app-border/50">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { animate } from 'animejs'

const brandingRef = ref<HTMLDivElement>()
const cardRef = ref<HTMLDivElement>()

onMounted(() => {
  // 尊重用户的减少动画偏好设置
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  // 入场动画（增强体验，失败不影响显示）
  try {
    animate(brandingRef.value!, {
      translateY: [16, 0],
      opacity: [0, 1],
      duration: 500,
      easing: 'out(2)',
    })
    animate(cardRef.value!, {
      translateY: [24, 0],
      opacity: [0, 1],
      duration: 600,
      delay: 150,
      easing: 'out(2)',
    })
  } catch {
    // 静默降级，内容已通过 CSS 可见
  }
})
</script>
