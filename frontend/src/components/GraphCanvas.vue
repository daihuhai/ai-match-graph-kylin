<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { GraphData } from '@/types/graph'
import RelationGraph from 'relation-graph-vue3'
import type { RGOptions, RelationGraphComponent, RGNode, RGLine } from 'relation-graph-vue3'
// Remove CSS import, letting the component handle it or importing from the correct path if needed
// import 'relation-graph/vue3/relation-graph.css'

const props = defineProps<{
  data: GraphData
  layout: 'force' | 'radial' | 'dagre'
}>()

const emit = defineEmits<{
  (e: 'nodeClick', payload: { id: string }): void
  (e: 'canvasClick'): void
}>()

const graphRef = ref<RelationGraphComponent | null>(null)

const graphOptions = computed<RGOptions>(() => {
  let layoutName = 'force'
  if (props.layout === 'radial') layoutName = 'center'
  if (props.layout === 'dagre') layoutName = 'tree'

  return {
    allowSwitchLineShape: true,
    allowSwitchJunctionPoint: true,
    defaultJunctionPoint: 'border',
    layouts: [
      {
        label: '自动布局',
        layoutName,
        layoutClassName: 'seeks-layout-' + layoutName,
        distance_coeff: props.layout === 'dagre' ? 1.5 : 2.5, // 调整节点间距
        ...(props.layout === 'dagre' ? { from: 'left', min_per_width: 80, max_per_width: 120 } : {}),
      },
    ],
    defaultNodeShape: 0, // 圆形
    defaultNodeWidth: 48, // 缩小节点，因为文字在外部
    defaultNodeHeight: 48,
    defaultLineShape: 6, // 贝塞尔曲线
    defaultLineColor: '#cbd5e1',
    defaultNodeFontColor: '#1e293b',
    defaultLineFontColor: '#64748b',
    defaultLineWidth: 1.5,
  }
})

const getColorTheme = (type: string) => {
  switch (type) {
    case 'Person': return { bg: '#e6f4ff', border: '#1677ff', text: '#0958d9', icon: '👤' }
    case 'Job': return { bg: '#fff7e6', border: '#fa8c16', text: '#d46b08', icon: '💼' }
    case 'Skill': return { bg: '#f6ffed', border: '#52c41a', text: '#389e0d', icon: '🎯' }
    case 'Project': return { bg: '#f9f0ff', border: '#722ed1', text: '#531dab', icon: '🚀' }
    default: return { bg: '#f5f5f5', border: '#8c8c8c', text: '#595959', icon: '📌' }
  }
}

const mapDataToRelationGraph = (data: GraphData) => {
  const nodes = data.nodes.map(n => {
    const type = n.type as string
    const theme = getColorTheme(type)
    
    // Apply custom styling from the wrapper if available (for dimming/highlighting)
    const extraStyle = n.style ?? {}
    const opacity = extraStyle.opacity !== undefined ? extraStyle.opacity : 1
    const lineWidth = extraStyle.lineWidth !== undefined ? extraStyle.lineWidth : 2
    const isSelected = extraStyle.stroke === 'selected'

    return {
      id: n.id,
      text: n.label,
      color: theme.bg,
      borderColor: isSelected ? '#111827' : theme.border,
      borderWidth: lineWidth,
      width: type === 'Job' ? 56 : 48,
      height: type === 'Job' ? 56 : 48,
      opacity,
      data: { ...n, theme, isSelected },
    }
  })

  const lines = data.edges.map(e => {
    const extraStyle = e.style ?? {}
    const opacity = extraStyle.opacity !== undefined ? extraStyle.opacity : 1
    const isFocus = opacity > 0.5

    return {
      from: e.source,
      to: e.target,
      text: e.relation,
      color: isFocus ? '#94a3b8' : '#e2e8f0', // 结合原有颜色和透明度
      lineWidth: isFocus ? 2 : 1.5,
      fontColor: isFocus ? '#475569' : '#94a3b8',
      data: e,
    }
  })

  return { rootId: nodes.length > 0 ? nodes[0].id : '', nodes, lines }
}

const showGraph = async () => {
  if (!graphRef.value) return
  const rgData = mapDataToRelationGraph(props.data)
  await graphRef.value.setJsonData(rgData)
}

watch(
  () => props.data,
  () => {
    showGraph()
  },
  { deep: true }
)

watch(
  () => props.layout,
  () => {
    showGraph()
  }
)

onMounted(() => {
  showGraph()
})

const onNodeClick = (nodeObject: RGNode) => {
  emit('nodeClick', { id: nodeObject.id })
}

const onCanvasClick = () => {
  emit('canvasClick')
}

const focusNode = (id: string) => {
  if (!graphRef.value) return
  // relation-graph-vue3 的组件实例暴露了 getInstance 方法
  const instance = ('getInstance' in graphRef.value)
    ? (graphRef.value as unknown as { getInstance: () => { focusNodeById: (id: string) => void } }).getInstance()
    : graphRef.value
  if (instance && typeof (instance as { focusNodeById?: (id: string) => void }).focusNodeById === 'function') {
    ;(instance as { focusNodeById: (id: string) => void }).focusNodeById(id)
  }
}

defineExpose({ focusNode })
</script>

<template>
  <div class="h-[560px] w-full overflow-hidden rounded-xl border border-app-border bg-app-bg dark:border-zinc-800 dark:bg-zinc-950 relative">
    <RelationGraph
      ref="graphRef"
      :options="graphOptions"
      @node-click="onNodeClick"
      @canvas-click="onCanvasClick"
    >
      <template #node="{ node: rawNode }">
        <div 
          class="flex flex-col items-center justify-center w-full h-full relative cursor-pointer group"
          :style="{ opacity: (rawNode as any).opacity ?? 1 }"
        >
          <!-- Node Circle -->
          <div 
            class="flex items-center justify-center rounded-full transition-all duration-300 bg-app-panel"
            :class="[(rawNode as any).data?.isSelected ? 'shadow-[0_0_0_4px_rgba(17,24,39,0.1)] scale-110' : 'shadow-sm group-hover:shadow-md']"
            :style="{
              backgroundColor: (rawNode as any).color,
              border: `${(rawNode as any).borderWidth}px solid ${(rawNode as any).borderColor}`,
              width: '100%',
              height: '100%'
            }"
          >
            <span class="text-xl" :style="{ color: (rawNode as any).data?.theme?.text }">
              {{ (rawNode as any).data?.theme?.icon }}
            </span>
          </div>
          
          <!-- Label Outside -->
          <div 
            class="absolute top-[calc(100%+6px)] w-max max-w-[140px] text-center text-xs px-2 py-1 rounded-md bg-app-panel/90 backdrop-blur shadow-sm border border-app-border whitespace-normal break-words leading-tight transition-all duration-300"
            :style="{ 
              color: (rawNode as any).data?.isSelected ? '#111827' : '#475569',
              fontWeight: (rawNode as any).data?.isSelected ? '600' : '500',
              zIndex: (rawNode as any).data?.isSelected ? 10 : 1,
              borderColor: (rawNode as any).data?.isSelected ? '#e5e7eb' : 'transparent'
            }"
          >
            {{ (rawNode as any).text }}
          </div>
        </div>
      </template>
    </RelationGraph>
  </div>
</template>
