import { http } from '@/api/http'
import { mockDocument } from '@/api/mock'
import { isMockEnabled } from '@/api/env'
import type { DocType, ParseResultVO } from '@/types/document'

export async function uploadDocument(payload: { file: File; docType: DocType }): Promise<{ docId: string }> {
  if (isMockEnabled()) {
    const fileType = payload.file.name.toLowerCase().endsWith('.pdf') ? 'PDF' : 'DOC'
    return mockDocument.upload({ fileName: payload.file.name, fileType, docType: payload.docType })
  }

  const form = new FormData()
  form.append('file', payload.file)
  form.append('docType', payload.docType)
  return http.post('/document/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120_000,
  })
}

export async function getDocumentStatus(docId: string): Promise<{ id: string; status: string }> {
  if (isMockEnabled()) return mockDocument.status(docId)
  return http.get(`/document/${encodeURIComponent(docId)}/status`)
}

export async function getParseResult(docId: string): Promise<ParseResultVO> {
  if (isMockEnabled()) return mockDocument.result(docId)
  return http.get(`/document/${encodeURIComponent(docId)}/result`)
}
