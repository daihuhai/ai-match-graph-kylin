<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { GraphData } from '@/types/graph'
import { Graph } from '@antv/g6'

const props = defineProps<{
  data: GraphData
  layout: 'force' | 'radial' | 'dagre'
}>()

const emit = defineEmits<{
  (e: 'nodeClick', payload: { id: string }): void
  (e: 'canvasClick'): void
}>()

const el = ref<HTMLDivElement | null>(null)
let graph: any = null
let ro: ResizeObserver | null = null
let rafId: number | null = null

const layoutCfg = computed(() => {
  if (props.layout === 'radial') return { type: 'radial', unitRadius: 80, preventOverlap: true }
  if (props.layout === 'dagre') return { type: 'dagre', rankdir: 'LR', nodesep: 24, ranksep: 36 }
  return { type: 'force', preventOverlap: true, linkDistance: 140 }
})

const updateSize = () => {
  if (!el.value) return
  if (!graph) return
  const rect = el.value.getBoundingClientRect()
  const w = Math.max(1, Math.floor(rect.width))
  const h = Math.max(1, Math.floor(rect.height))
  if (typeof graph.resize === 'function') graph.resize(w, h)
  else graph.setSize([w, h])
}

const ensureGraph = () => {
  if (!el.value) return
  if (graph) return
  const rect = el.value.getBoundingClientRect()

  graph = new Graph({
    container: el.value,
    animation: false,
    width: Math.max(1, Math.floor(rect.width)),
    height: Math.max(1, Math.floor(rect.height)),
    data: props.data as any,
    layout: layoutCfg.value as any,
    node: {
      type: 'circle',
      style: (d: any) => {
        const type = d?.type as string
        const fill =
          type === 'Person'
            ? '#1677ff'
            : type === 'Job'
              ? '#fa8c16'
              : type === 'Skill'
                ? '#52c41a'
                : type === 'Project'
                  ? '#722ed1'
                  : '#8c8c8c'
        const base = {
          fill,
          stroke: '#ffffff',
          lineWidth: 2,
          labelText: d?.label,
          labelFill: '#0a0a0a',
          labelPlacement: 'bottom',
          size: type === 'Job' ? 40 : 32,
        }
        const extra = d?.style && typeof d.style === 'object' ? d.style : {}
        return { ...base, ...extra }
      },
    },
    edge: {
      style: (d: any) => {
        const base = { stroke: 'rgba(24,24,27,.35)', endArrow: true }
        const extra = d?.style && typeof d.style === 'object' ? d.style : {}
        return { ...base, ...extra }
      },
    },
    behaviors: [
      'drag-canvas',
      'zoom-canvas',
      {
        type: 'drag-element',
        key: 'drag-element',
        enableAnimation: false,
        shadow: false,
      },
    ],
  })

  graph.on('node:click', (evt: any) => {
    const id = evt?.data?.data?.id ?? evt?.itemId ?? evt?.item?.id ?? evt?.target?.id
    if (typeof id === 'string') emit('nodeClick', { id })
  })

  graph.on('canvas:click', () => emit('canvasClick'))

  graph.render()
}

onMounted(() => {
  ensureGraph()
  if (el.value) {
    ro = new ResizeObserver(() => {
      if (rafId) window.cancelAnimationFrame(rafId)
      rafId = window.requestAnimationFrame(() => updateSize())
    })
    ro.observe(el.value)
  }
})

watch(
  () => props.data,
  (d) => {
    if (!graph) ensureGraph()
    graph.setData(d as any)
    updateSize()
    graph.render()
  },
  { deep: true },
)

watch(
  () => props.layout,
  () => {
    if (!graph) return
    graph.setLayout(layoutCfg.value as any)
    updateSize()
    graph.render()
  },
)

onBeforeUnmount(() => {
  if (ro && el.value) ro.unobserve(el.value)
  ro = null
  if (rafId) window.cancelAnimationFrame(rafId)
  rafId = null
  if (graph) graph.destroy()
  graph = null
})

const focusNode = (id: string) => {
  if (!graph) return
  const anyGraph = graph as any
  if (typeof anyGraph.focusElement === 'function') anyGraph.focusElement(id)
  else if (typeof anyGraph.centerElement === 'function') anyGraph.centerElement(id)
}

defineExpose({ focusNode })
</script>

<template>
  <div ref="el" class="h-[560px] w-full overflow-hidden rounded-xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950" />
</template>
