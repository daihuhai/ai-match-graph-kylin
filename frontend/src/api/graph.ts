import { http } from '@/api/http'
import type { GraphData } from '@/types/graph'

export async function getPersonGraph(personId: string): Promise<GraphData> {
  return http.get(`/graph/person/${encodeURIComponent(personId)}`)
}

export async function getJobGraph(jobId: string): Promise<GraphData> {
  return http.get(`/graph/job/${encodeURIComponent(jobId)}`)
}

export async function expandGraphNode(payload: { subject: 'person' | 'job'; nodeId: string }): Promise<GraphData> {
  return http.post('/graph/expand', payload)
}