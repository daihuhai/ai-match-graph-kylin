import { http } from '@/api/http'
import type { DocType, ParseResultVO } from '@/types/document'

export async function uploadDocument(payload: {
  file: File
  docType: DocType
  publishToTalentPool?: boolean
  companyAccount?: string
}): Promise<{ docId: string }> {
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
  return http.post(`/document/${encodeURIComponent(docId)}/talent-pool`, { publish: true, companyAccount })
}

export async function unpublishResumeFromTalentPool(
  docId: string,
  companyAccount: string,
): Promise<{ docId: string; talentPoolPublished: boolean; companyAccount: string }> {
  return http.post(`/document/${encodeURIComponent(docId)}/talent-pool`, { publish: false, companyAccount })
}

export async function listCompanyTargets(): Promise<Array<{ account: string }>> {
  return http.get('/document/company-targets')
}

export async function getDocumentStatus(docId: string): Promise<{ id: string; status: string }> {
  return http.get(`/document/${encodeURIComponent(docId)}/status`)
}

export async function getParseResult(docId: string): Promise<ParseResultVO> {
  return http.get(`/document/${encodeURIComponent(docId)}/result`)
}

export async function getOriginalDocumentBlob(docId: string): Promise<Blob> {
  return http.get(`/document/${encodeURIComponent(docId)}/original`, { responseType: 'blob' })
}