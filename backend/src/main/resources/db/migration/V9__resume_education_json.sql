-- V9: 添加教育经历JSON字段（简历解析结构化教育信息持久化）

ALTER TABLE aimap_document
  ADD COLUMN IF NOT EXISTS resume_education_json JSON NULL;
