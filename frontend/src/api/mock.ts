import type { DocStatus, DocType, DocFileVO, ParseResultVO } from '@/types/document'
import type { GraphData } from '@/types/graph'
import type { MatchDetailVO, MatchListItem } from '@/types/match'
import { getDataStorage } from '@/utils/storage'

const genId = (prefix: string) => `${prefix}-${Math.random().toString(16).slice(2, 10)}`

type DocTask = {
  doc: DocFileVO
  startedAt: number
  result: ParseResultVO
}

const DOC_TASKS_KEY = 'aimap.mock.docTasks'

const readDocTasks = (): Record<string, DocTask> => {
  const storage = getDataStorage()
  const raw = storage.getItem(DOC_TASKS_KEY)
  if (!raw) return {}
  try {
    const v = JSON.parse(raw) as Record<string, DocTask>
    return v && typeof v === 'object' ? v : {}
  } catch {
    storage.removeItem(DOC_TASKS_KEY)
    return {}
  }
}

const writeDocTasks = (tasks: Record<string, DocTask>) => {
  getDataStorage().setItem(DOC_TASKS_KEY, JSON.stringify(tasks))
}

export const mockDocument = {
  async upload(payload: { fileName: string; fileType: 'DOC' | 'PDF'; docType: DocType }) {
    const id = genId('doc')
    const now = Date.now()
    const doc: DocFileVO = {
      id,
      fileName: payload.fileName,
      fileType: payload.fileType,
      docType: payload.docType,
      status: 'PENDING',
      createdAt: new Date(now).toISOString(),
    }
    const result: ParseResultVO = {
      docId: id,
      status: 'PENDING',
      resultJson: {
        skills: ['Java', 'Spring Boot', 'Vue 3', 'TypeScript', 'SQL'],
        education: [{ school: '某高校', degree: '本科', major: '计算机科学与技术' }],
        projects: [{ name: '能力图谱系统', summary: '解析简历并构建技能图谱，输出可解释匹配结果' }],
        ...(payload.docType === 'RESUME'
          ? {
              resumeCritique: '【优势】\n- 工程基础扎实\n',
              hollandRiasec: { R: 35, I: 48, A: 30, S: 52, E: 38, C: 44 },
            }
          : {
              jobCritique: '摘要：示例岗位\n',
              jobHollandRiasec: { R: 32, I: 50, A: 28, S: 40, E: 42, C: 46 },
            }),
      },
      evidences: [{ field: 'skills', page: 1, text: '熟悉 Vue3/TS、Spring Boot、SQL...' }],
      ...(payload.docType === 'RESUME'
        ? { canPublishToTalentPool: true, talentPoolPublished: false }
        : {}),
    }
    const tasks = readDocTasks()
    tasks[id] = { doc, startedAt: now, result }
    writeDocTasks(tasks)
    return { docId: id }
  },
  async status(docId: string) {
    const tasks = readDocTasks()
    const task = tasks[docId]
    if (!task) {
      const err = new Error('任务不存在')
      ;(err as any).code = 'NOT_FOUND'
      throw err
    }
    const elapsed = Date.now() - task.startedAt
    const status: DocStatus =
      elapsed < 1500 ? 'PENDING' : elapsed < 4500 ? 'PROCESSING' : 'DONE'
    task.doc.status = status
    task.result.status = status
    tasks[docId] = task
    writeDocTasks(tasks)
    return { id: task.doc.id, status }
  },
  async result(docId: string) {
    const tasks = readDocTasks()
    const task = tasks[docId]
    if (!task) throw new Error('任务不存在')
    await mockDocument.status(docId)
    const base = { ...task.result }
    if (task.doc.docType === 'RESUME') {
      base.canPublishToTalentPool = true
      base.talentPoolPublished = base.talentPoolPublished ?? false
    }
    return base
  },
  async publishTalentPool(docId: string) {
    const tasks = readDocTasks()
    const task = tasks[docId]
    if (!task) {
      const err = new Error('任务不存在')
      ;(err as any).code = 'NOT_FOUND'
      throw err
    }
    if (task.doc.docType !== 'RESUME') {
      throw new Error('仅简历可上传至人才库')
    }
    task.result = { ...task.result, talentPoolPublished: true }
    tasks[docId] = task
    writeDocTasks(tasks)
    return { docId, talentPoolPublished: true }
  },
}

export const mockGraph = {
  async personGraph(personId: string): Promise<GraphData> {
    return {
      nodes: [
        { id: personId, label: '个人', type: 'Person' },
        { id: 'skill-vue', label: 'Vue 3', type: 'Skill' },
        { id: 'skill-ts', label: 'TypeScript', type: 'Skill' },
        { id: 'skill-spring', label: 'Spring Boot', type: 'Skill' },
        { id: 'proj-1', label: '图谱系统', type: 'Project' },
      ],
      edges: [
        { id: genId('e'), source: personId, target: 'skill-vue', relation: 'HAS_SKILL' },
        { id: genId('e'), source: personId, target: 'skill-ts', relation: 'HAS_SKILL' },
        { id: genId('e'), source: personId, target: 'skill-spring', relation: 'HAS_SKILL' },
        { id: genId('e'), source: 'proj-1', target: 'skill-vue', relation: 'APPLIED_IN' },
        { id: genId('e'), source: 'proj-1', target: 'skill-ts', relation: 'APPLIED_IN' },
      ],
    }
  },
  async jobGraph(jobId: string): Promise<GraphData> {
    return {
      nodes: [
        { id: jobId, label: '岗位', type: 'Job' },
        { id: 'skill-vue', label: 'Vue 3', type: 'Skill' },
        { id: 'skill-ts', label: 'TypeScript', type: 'Skill' },
        { id: 'skill-g6', label: 'AntV G6', type: 'Skill' },
      ],
      edges: [
        { id: genId('e'), source: jobId, target: 'skill-vue', relation: 'REQUIRES_SKILL' },
        { id: genId('e'), source: jobId, target: 'skill-ts', relation: 'REQUIRES_SKILL' },
        { id: genId('e'), source: jobId, target: 'skill-g6', relation: 'REQUIRES_SKILL' },
      ],
    }
  },
  async expand(payload: { subject: 'person' | 'job'; nodeId: string }): Promise<GraphData> {
    const nodeId = payload.nodeId
    const addSkill = (name: string) => ({ id: `skill-${name.toLowerCase().replace(/\s+/g, '-')}`, label: name, type: 'Skill' as const })

    if (nodeId.startsWith('skill-')) {
      const related = [
        addSkill('ECharts'),
        addSkill('Pinia'),
        addSkill('Axios'),
      ]
      const edges = related.map((n) => ({
        id: genId('e'),
        source: nodeId,
        target: n.id,
        relation: 'DEPENDS_ON' as const,
      }))
      return { nodes: related, edges }
    }

    if (payload.subject === 'person') {
      const nodes = [
        { id: 'cat-frontend', label: '前端', type: 'Category' as const },
        { id: 'cat-backend', label: '后端', type: 'Category' as const },
      ]
      const edges = [
        { id: genId('e'), source: 'skill-vue', target: 'cat-frontend', relation: 'BELONGS_TO' as const },
        { id: genId('e'), source: 'skill-ts', target: 'cat-frontend', relation: 'BELONGS_TO' as const },
        { id: genId('e'), source: 'skill-spring', target: 'cat-backend', relation: 'BELONGS_TO' as const },
      ]
      return { nodes, edges }
    }

    const nodes = [
      { id: 'cat-graph', label: '图谱能力', type: 'Category' as const },
      { id: 'skill-webgl', label: 'WebGL', type: 'Skill' as const },
    ]
    const edges = [
      { id: genId('e'), source: 'skill-g6', target: 'cat-graph', relation: 'BELONGS_TO' as const },
      { id: genId('e'), source: 'skill-g6', target: 'skill-webgl', relation: 'DEPENDS_ON' as const },
    ]
    return { nodes, edges }
  },
}

export const mockMatch = {
  async recommendJobs(minScore = 0): Promise<MatchListItem[]> {
    const rows: MatchListItem[] = [
      { recordId: 'rec-job-001', title: '前端开发（Vue3）', org: '某科技公司', score: 86 },
      { recordId: 'rec-job-002', title: '全栈开发（Java+Vue）', org: '某平台团队', score: 79 },
    ]
    return rows.filter((r) => r.score >= minScore)
  },
  async recommendCandidates(minScore = 0): Promise<MatchListItem[]> {
    const rows: MatchListItem[] = [
      { recordId: 'rec-cand-001', title: '候选人：张三', org: '3 年经验', score: 84 },
      { recordId: 'rec-cand-002', title: '候选人：李四', org: '5 年经验', score: 77 },
    ]
    return rows.filter((r) => r.score >= minScore)
  },
  async detail(recordId: string): Promise<MatchDetailVO> {
    const isJob = recordId.startsWith('rec-job-')
    const person =
      recordId.endsWith('002')
        ? { r: 30, i: 55, a: 35, s: 40, e: 25, c: 60 }
        : { r: 35, i: 45, a: 30, s: 55, e: 40, c: 35 }
    const target = isJob
      ? recordId.endsWith('002')
        ? { r: 45, i: 50, a: 25, s: 30, e: 40, c: 55 }
        : { r: 30, i: 40, a: 35, s: 50, e: 45, c: 30 }
      : recordId.endsWith('002')
        ? { r: 40, i: 45, a: 30, s: 55, e: 35, c: 40 }
        : { r: 35, i: 50, a: 28, s: 45, e: 38, c: 42 }
    const dist =
      Math.abs(person.r - target.r) +
      Math.abs(person.i - target.i) +
      Math.abs(person.a - target.a) +
      Math.abs(person.s - target.s) +
      Math.abs(person.e - target.e) +
      Math.abs(person.c - target.c)
    const similarity = Math.max(0, Math.min(100, Math.round(100 - dist / 6)))
    return {
      recordId,
      score: isJob ? (recordId.endsWith('002') ? 79 : 86) : recordId.endsWith('002') ? 77 : 84,
      scoreBreakdown: {
        skillCoverage: 42,
        levelMatch: 20,
        vectorSim: 12,
        graphReasoning: 12,
      },
      matchedSkills: [
        { name: 'Vue 3', requiredLevel: 3, personLevel: 3 },
        { name: 'TypeScript', requiredLevel: 3, personLevel: 2 },
      ],
      missingSkills: [{ name: 'AntV G6', requiredLevel: 2, gap: 2 }],
      rationales: [
        '技能覆盖率较高：核心技能（Vue3/TS）匹配，但可视化图谱能力存在缺口。',
        '图谱推理命中：项目经历与技能节点存在直接关联边，提升可解释性得分。',
      ],
      evidences: [
        { type: 'TEXT', field: 'skills', snippet: '熟悉 Vue3/TS、Spring Boot、SQL...', weight: 0.35 },
        { type: 'GRAPH', field: 'HAS_SKILL', snippet: 'Person -> Vue 3 / TypeScript', weight: 0.25 },
        { type: 'VECTOR', field: 'embedding', snippet: '语义相似度（文本向量）', weight: 0.4 },
      ],
      riasec: { person, target, similarity },
      suggestions: [
        '补齐图谱可视化能力（G6 基础与交互），优先掌握布局与事件系统',
        '完善工程化规范：路由守卫、错误兜底与性能分包',
      ],
    }
  },
}
