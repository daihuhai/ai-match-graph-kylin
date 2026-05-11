import { http } from '@/api/http'
import { mockMatch } from '@/api/mock'
import { isMockEnabled } from '@/api/env'
import type { MatchDetailVO, MatchListItem } from '@/types/match'

export async function recommendJobs(minScore = 0): Promise<MatchListItem[]> {
  if (isMockEnabled()) return mockMatch.recommendJobs()
  return http.post('/match/recommend-jobs', { minScore })
}

export async function recommendCandidates(minScore = 0): Promise<MatchListItem[]> {
  if (isMockEnabled()) return mockMatch.recommendCandidates()
  return http.post('/match/recommend-candidates', { minScore })
}

export async function getMatchDetail(recordId: string): Promise<MatchDetailVO> {
  if (isMockEnabled()) return mockMatch.detail(recordId)
  return http.get(`/match/${encodeURIComponent(recordId)}/detail`)
}
