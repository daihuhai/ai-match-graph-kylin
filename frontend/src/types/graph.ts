export interface GraphNode {
  id: string
  label: string
  type: 'Person' | 'Job' | 'Skill' | 'Project' | 'Company' | 'Category'
  props?: Record<string, unknown>
}

export interface GraphEdge {
  id: string
  source: string
  target: string
  relation: 'HAS_SKILL' | 'REQUIRES_SKILL' | 'DEPENDS_ON' | 'BELONGS_TO' | 'APPLIED_IN' | 'PUBLISHES'
  props?: Record<string, unknown>
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}
