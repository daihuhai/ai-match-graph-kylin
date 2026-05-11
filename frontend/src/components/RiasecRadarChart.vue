<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  title: string
  personValues: number[]
  targetValues: number[]
  labels?: { person?: string; target?: string }
}>()

const el = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

const render = () => {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  else chart.resize()
  const indicator = [
    { name: 'R 现实型', max: 100 },
    { name: 'I 研究型', max: 100 },
    { name: 'A 艺术型', max: 100 },
    { name: 'S 社会型', max: 100 },
    { name: 'E 企业型', max: 100 },
    { name: 'C 常规型', max: 100 },
  ]

  chart.setOption({
    title: { text: props.title, left: 'center', top: 8, textStyle: { fontSize: 12 } },
    tooltip: {},
    legend: {
      top: 30,
      data: [props.labels?.person || '个人', props.labels?.target || '目标'],
      textStyle: { fontSize: 11 },
    },
    radar: { indicator, radius: '62%' },
    series: [
      {
        type: 'radar',
        data: [
          { name: props.labels?.person || '个人', value: props.personValues, areaStyle: { opacity: 0.12 } },
          { name: props.labels?.target || '目标', value: props.targetValues, areaStyle: { opacity: 0.08 } },
        ],
      },
    ],
  })
  chart.resize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', render)
})

watch(() => [props.personValues, props.targetValues], render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', render)
  if (chart) chart.dispose()
  chart = null
})
</script>

<template>
  <div ref="el" class="h-[320px] w-full rounded-xl border border-zinc-200 bg-white" />
</template>
