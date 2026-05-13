import { http } from '@/api/http'
import { mockDocument } from '@/api/mock'
import { isMockEnabled } from '@/api/env'
import type { DocType, ParseResultVO } from '@/types/document'

export async function uploadDocument(payload: {
  file: File
  docType: DocType
  publishToTalentPool?: boolean
  companyAccount?: string
}): Promise<{ docId: string }> {
  if (isMockEnabled()) {
    const fileType = payload.file.name.toLowerCase().endsWith('.pdf') ? 'PDF' : 'DOC'
    return mockDocument.upload({ fileName: payload.file.name, fileType, docType: payload.docType })
  }

  const form = new FormData()
  form.append('file', payload.file)
  form.append('docType', payload.docType)
  form.append('publishToTalentPool', payload.publishToTalentPool ? 'true' : 'false')
  if (payload.companyAccount) form.append('companyAccount', payload.companyAccount)
  return http.post('/document/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120_000,
  })
}

export async function publishResumeToTalentPool(
  docId: string,
  companyAccount: string,
): Promise<{ docId: string; talentPoolPublished: boolean; companyAccount: string }> {
  if (isMockEnabled()) {
    const res = await mockDocument.publishTalentPool(docId, true, companyAccount)
    return { ...res, companyAccount }
  }
  return http.post(`/document/${encodeURIComponent(docId)}/talent-pool`, { publish: true, companyAccount })
}

export async function unpublishResumeFromTalentPool(
  docId: string,
  companyAccount: string,
): Promise<{ docId: string; talentPoolPublished: boolean; companyAccount: string }> {
  if (isMockEnabled()) {
    const res = await mockDocument.publishTalentPool(docId, false, companyAccount)
    return { ...res, companyAccount }
  }
  return http.post(`/document/${encodeURIComponent(docId)}/talent-pool`, { publish: false, companyAccount })
}

export async function listCompanyTargets(): Promise<Array<{ account: string }>> {
  if (isMockEnabled()) return [{ account: 'demo-company' }]
  return http.get('/document/company-targets')
}

export async function getDocumentStatus(docId: string): Promise<{ id: string; status: string }> {
  if (isMockEnabled()) return mockDocument.status(docId)
  return http.get(`/document/${encodeURIComponent(docId)}/status`)
}

export async function getParseResult(docId: string): Promise<ParseResultVO> {
  if (isMockEnabled()) return mockDocument.result(docId)
  return http.get(`/document/${encodeURIComponent(docId)}/result`)
}

export async function getOriginalDocumentBlob(docId: string): Promise<Blob> {
  if (isMockEnabled()) throw new Error('Mock 模式暂不支持原始文档预览')
  return http.get(`/document/${encodeURIComponent(docId)}/original`, { responseType: 'blob' })
}
