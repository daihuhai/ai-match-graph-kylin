export type DocType = 'RESUME' | 'JOB_DESC'
export type DocStatus = 'UPLOADING' | 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED'

export interface DocFileVO {
  id: string
  fileName: string
  fileType: 'DOC' | 'PDF'
  docType: DocType
  status: DocStatus
  createdAt: string
}

export interface ParseResultVO {
  docId: string
  status: DocStatus
  resultJson: unknown
  evidences?: Array<{ field: string; page?: number; text?: string }>
}

