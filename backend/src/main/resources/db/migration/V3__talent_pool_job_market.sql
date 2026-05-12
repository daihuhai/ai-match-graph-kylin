-- 个人「人才库」与企业「人才市场」：扩展文档与重命名目录表
ALTER TABLE aimap_document
  ADD COLUMN resume_skills_json JSON NULL AFTER resume_holland_json;

ALTER TABLE aimap_candidate_catalog
  ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'SEED',
  ADD COLUMN document_id VARCHAR(40) NULL,
  ADD COLUMN owner_user_type VARCHAR(16) NULL,
  ADD COLUMN owner_account VARCHAR(128) NULL,
  ADD COLUMN in_pool TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE aimap_job_catalog
  ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'SEED',
  ADD COLUMN document_id VARCHAR(40) NULL,
  ADD COLUMN owner_user_type VARCHAR(16) NULL,
  ADD COLUMN owner_account VARCHAR(128) NULL;

RENAME TABLE aimap_candidate_catalog TO aimap_talent_pool;
RENAME TABLE aimap_job_catalog TO aimap_job_market;
