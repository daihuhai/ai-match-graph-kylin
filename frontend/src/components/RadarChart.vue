<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(defineProps<{
  title?: string
  indicators: Array<{ name: string; max: number }>
  values: number[]
}>(), {
  title: '',
})

const el = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

const render = () => {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  else chart.resize()
  chart.setOption({
    title: props.title ? { text: props.title, left: 'center', top: 8, textStyle: { fontSize: 12 } } : undefined,
    tooltip: {},
    radar: { indicator: props.indicators, radius: '62%' },
    series: [
      {
        type: 'radar',
        data: [{ value: props.values }],
        areaStyle: { opacity: 0.18 },
      },
    ],
  })
  chart.resize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', render)
})

watch(() => [props.indicators, props.values], render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', render)
  if (chart) chart.dispose()
  chart = null
})
</script>

<template>
  <div ref="el" class="h-[280px] w-full rounded-xl border border-app-border bg-app-panel" />
</template>
