ALTER TABLE aimap_talent_pool
  ADD COLUMN target_company_user_type VARCHAR(16) NULL,
  ADD COLUMN target_company_account VARCHAR(128) NULL;

UPDATE aimap_talent_pool
SET target_company_user_type = 'COMPANY',
    target_company_account = '__legacy_global__'
WHERE target_company_user_type IS NULL;

CREATE INDEX idx_talent_pool_company_scope
  ON aimap_talent_pool (target_company_user_type, target_company_account, in_pool, record_id);
