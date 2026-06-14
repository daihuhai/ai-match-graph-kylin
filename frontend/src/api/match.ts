import { http } from '@/api/http'
import type { MatchDetailVO, MatchListItem } from '@/types/match'

export async function recommendJobs(minScore = 0): Promise<MatchListItem[]> {
  return http.post('/match/recommend-jobs', { minScore })
}

export async function recommendCandidates(minScore = 0): Promise<MatchListItem[]> {
  return http.post('/match/recommend-candidates', { minScore })
}

/** 简历解析页：用该文档的霍兰德画像与「人才市场」岗位（含企业上传 JD）算分，无需加入人才库 */
export async function jobMarketMatchesForDocument(docId: string, minScore = 0): Promise<MatchListItem[]> {
  return http.get(`/match/job-market/for-document/${encodeURIComponent(docId)}`, { params: { minScore } })
}

export async function getMatchDetail(recordId: string): Promise<MatchDetailVO> {
  return http.get(`/match/${encodeURIComponent(recordId)}/detail`)
}