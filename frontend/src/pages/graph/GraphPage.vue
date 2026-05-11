<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import GraphCanvas from '@/components/GraphCanvas.vue'
import { expandGraphNode, getJobGraph, getPersonGraph } from '@/api/graph'
import type { GraphData, GraphNode } from '@/types/graph'
import { isWebGLAvailable } from '@/utils/webgl'

const route = useRoute()
const graphRef = ref<InstanceType<typeof GraphCanvas> | null>(null)

const subjectId = computed(() => String(route.params.subjectId || ''))
const isCompany = computed(() => route.path.startsWith('/company'))
const subject = computed<'person' | 'job'>(() => (isCompany.value ? 'job' : 'person'))
const title = computed(() => (isCompany.value ? '职位能力图谱' : '个人能力图谱'))

const layout = ref<'force' | 'radial' | 'dagre'>('radial')
const baseData = ref<GraphData>({ nodes: [], edges: [] })
const webglOk = ref(true)
const loading = ref(true)

const selectedId = ref<string>('')
const drawerOpen = ref(false)
const keyword = ref('')

type ExpansionInfo = { nodeIds: string[]; edgeIds: string[] }
const expandedByNode = ref<Record<string, ExpansionInfo>>({})
let originNodeIds = new Set<string>()
let originEdgeIds = new Set<string>()
const nodeRef = new Map<string, number>()
const edgeRef = new Map<string, number>()

const nodeMap = computed(() => {
  const m = new Map<string, GraphNode>()
  for (const n of baseData.value.nodes) m.set(n.id, n)
  return m
})

const selectedNode = computed(() => nodeMap.value.get(selectedId.value) ?? null)

const neighborSet = computed(() => {
  if (!selectedId.value) return new Set<string>()
  const set = new Set<string>([selectedId.value])
  for (const e of baseData.value.edges) {
    if (e.source === selectedId.value) set.add(e.target)
    if (e.target === selectedId.value) set.add(e.source)
  }
  return set
})

const decorated = computed<GraphData>(() => {
  const focus = neighborSet.value
  const shouldDim = focus.size > 0
  const nodes = baseData.value.nodes.map((n) => {
    if (!shouldDim) return n
    const isFocus = focus.has(n.id)
    const isSelected = n.id === selectedId.value
    const style = {
      opacity: isFocus ? 1 : 0.18,
      lineWidth: isSelected ? 4 : 2,
      stroke: isSelected ? '#111827' : '#ffffff',
    }
    return { ...n, style }
  })
  const edges = baseData.value.edges.map((e) => {
    if (!shouldDim) return e
    const onPath = focus.has(e.source) && focus.has(e.target)
    const style = { opacity: onPath ? 0.9 : 0.12 }
    return { ...e, style }
  })
  return { nodes, edges }
})

const suggestions = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return []
  return baseData.value.nodes
    .filter((n) => n.label.toLowerCase().includes(k))
    .slice(0, 8)
    .map((n) => ({ value: n.label, id: n.id }))
})

const load = async () => {
  loading.value = true
  selectedId.value = ''
  drawerOpen.value = false
  expandedByNode.value = {}
  nodeRef.clear()
  edgeRef.clear()
  try {
    baseData.value = isCompany.value ? await getJobGraph(subjectId.value) : await getPersonGraph(subjectId.value)
    originNodeIds = new Set(baseData.value.nodes.map((n) => n.id))
    originEdgeIds = new Set(baseData.value.edges.map((e) => e.id))
  } catch (e: any) {
    ElMessage.error(e?.message || '加载图谱失败')
  } finally {
    loading.value = false
  }
}

const onNodeClick = (payload: { id: string }) => {
  selectedId.value = payload.id
  drawerOpen.value = true
}

const onCanvasClick = () => {
  selectedId.value = ''
  drawerOpen.value = false
}

const onPick = (item: any) => {
  const id = item?.id as string | undefined
  if (!id) return
  selectedId.value = id
  drawerOpen.value = true
  graphRef.value?.focusNode(id)
}

const isExpanded = computed(() => Boolean(selectedId.value && expandedByNode.value[selectedId.value]))

const toggleExpand = async () => {
  const id = selectedId.value
  if (!id) return

  if (expandedByNode.value[id]) {
    const info = expandedByNode.value[id]
    delete expandedByNode.value[id]

    for (const nid of info.nodeIds) {
      const c = nodeRef.get(nid) ?? 0
      const next = c - 1
      if (next <= 0) nodeRef.delete(nid)
      else nodeRef.set(nid, next)
    }

    for (const eid of info.edgeIds) {
      const c = edgeRef.get(eid) ?? 0
      const next = c - 1
      if (next <= 0) edgeRef.delete(eid)
      else edgeRef.set(eid, next)
    }

    baseData.value = {
      nodes: baseData.value.nodes.filter((n) => originNodeIds.has(n.id) || (nodeRef.get(n.id) ?? 0) > 0),
      edges: baseData.value.edges.filter((e) => originEdgeIds.has(e.id) || (edgeRef.get(e.id) ?? 0) > 0),
    }
    return
  }

  try {
    const patch = await expandGraphNode({ subject: subject.value, nodeId: id })
    const nodeIds = new Set(baseData.value.nodes.map((n) => n.id))
    const edgeIds = new Set(baseData.value.edges.map((e) => e.id))

    const addNodes: GraphNode[] = []
    const infoNodeIds: string[] = []
    for (const n of patch.nodes) {
      if (originNodeIds.has(n.id)) continue
      const prev = nodeRef.get(n.id) ?? 0
      nodeRef.set(n.id, prev + 1)
      infoNodeIds.push(n.id)
      if (!nodeIds.has(n.id) && prev === 0) addNodes.push(n)
    }

    const addEdges = []
    const infoEdgeIds: string[] = []
    for (const e of patch.edges) {
      if (originEdgeIds.has(e.id)) continue
      const prev = edgeRef.get(e.id) ?? 0
      edgeRef.set(e.id, prev + 1)
      infoEdgeIds.push(e.id)
      if (!edgeIds.has(e.id) && prev === 0) addEdges.push(e)
    }

    expandedByNode.value[id] = { nodeIds: infoNodeIds, edgeIds: infoEdgeIds }
    baseData.value = {
      nodes: baseData.value.nodes.concat(addNodes),
      edges: baseData.value.edges.concat(addEdges),
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '展开失败')
  }
}

onMounted(async () => {
  webglOk.value = isWebGLAvailable()
  await load()
})
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never" class="border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div class="text-base font-semibold text-zinc-900 dark:text-zinc-100">{{ title }}</div>
          <div class="mt-1 text-sm text-zinc-600 dark:text-zinc-400">
            支持拖拽、缩放与布局切换；支持搜索定位与节点详情；可按需展开邻居（mock）。
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <el-tag v-if="!webglOk" type="warning">已进入兼容模式</el-tag>

          <el-autocomplete
            v-model="keyword"
            class="w-[260px]"
            :fetch-suggestions="(_, cb) => cb(suggestions)"
            placeholder="搜索节点（技能/项目/分类）"
            @select="onPick"
          />

          <el-select v-model="layout" class="w-[140px]">
            <el-option label="辐射" value="radial" />
            <el-option label="力导向" value="force" />
            <el-option label="层次" value="dagre" />
          </el-select>
          <el-button :loading="loading" @click="load">重置</el-button>
        </div>
      </div>
    </el-card>

    <div v-if="loading" class="text-sm text-zinc-600 dark:text-zinc-400">加载中...</div>
    <GraphCanvas v-else ref="graphRef" :data="decorated" :layout="layout" @node-click="onNodeClick" @canvas-click="onCanvasClick" />

    <el-drawer v-model="drawerOpen" title="节点详情" size="420px">
      <div v-if="!selectedNode" class="text-sm text-zinc-600 dark:text-zinc-400">请选择一个节点</div>
      <div v-else class="space-y-3">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ selectedNode.label }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ selectedNode.type }}</el-descriptions-item>
          <el-descriptions-item label="ID">{{ selectedNode.id }}</el-descriptions-item>
        </el-descriptions>

        <div class="flex gap-2">
          <el-button type="primary" @click="toggleExpand">{{ isExpanded ? '收起邻居' : '展开邻居' }}</el-button>
          <el-button @click="graphRef?.focusNode(selectedNode.id)">定位</el-button>
        </div>

        <div>
          <div class="text-sm font-semibold text-zinc-700 dark:text-zinc-200">邻居节点</div>
          <div class="mt-2 flex flex-wrap gap-2">
            <el-tag
              v-for="id in Array.from(neighborSet).filter((x) => x !== selectedNode.id)"
              :key="id"
              class="cursor-pointer"
              @click="onPick({ id })"
            >
              {{ nodeMap.get(id)?.label || id }}
            </el-tag>
            <div v-if="neighborSet.size <= 1" class="text-sm text-zinc-600 dark:text-zinc-400">暂无</div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
