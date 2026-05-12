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
  /** 个人简历：是否可将本次解析加入系统人才库（已登录个人账号） */
  canPublishToTalentPool?: boolean
  /** 个人简历：是否已在人才库中对企可见 */
  talentPoolPublished?: boolean
}

