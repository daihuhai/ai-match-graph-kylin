-- V8: 生产化约束增强 — 唯一索引、防重复投递、查询性能索引

-- 1. 手机号唯一索引（同一 userType 下手机号唯一，忽略空值）
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_phone_unique
  ON aimap_user (user_type, phone)
  WHERE phone IS NOT NULL;

-- 2. 邮箱唯一索引（同一 userType 下邮箱唯一，忽略空值）
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_unique
  ON aimap_user (user_type, email)
  WHERE email IS NOT NULL;

-- 3. 人才投递唯一约束：同一份简历文档对同一企业只能有一条有效记录
CREATE UNIQUE INDEX IF NOT EXISTS idx_talent_pool_delivery_unique
  ON aimap_talent_pool (document_id, target_company_user_type, target_company_account)
  WHERE in_pool = TRUE;

-- 4. 文档按所有者查询索引（个人端/企业端查看自己的文档列表）
CREATE INDEX IF NOT EXISTS idx_document_owner
  ON aimap_document (owner_user_type, owner_account, created_at DESC);

-- 5. 人才市场按企业范围查询索引
CREATE INDEX IF NOT EXISTS idx_job_market_owner
  ON aimap_job_market (owner_user_type, owner_account, record_id ASC);