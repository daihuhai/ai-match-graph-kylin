-- KingbaseES / PostgreSQL compatible

CREATE OR REPLACE FUNCTION aimap_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE aimap_user (
  id BIGSERIAL PRIMARY KEY,
  user_type VARCHAR(16) NOT NULL,
  account VARCHAR(128) NOT NULL,
  password VARCHAR(255) NOT NULL,
  resume_riasec_json JSON NULL,
  job_riasec_json JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_user_type_account UNIQUE (user_type, account)
);

CREATE TRIGGER trg_aimap_user_updated_at
  BEFORE UPDATE ON aimap_user
  FOR EACH ROW EXECUTE PROCEDURE aimap_touch_updated_at();

CREATE TABLE aimap_document (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  file_name VARCHAR(512) NOT NULL,
  file_type VARCHAR(16) NOT NULL,
  doc_type VARCHAR(32) NOT NULL,
  started_at_ms BIGINT NOT NULL,
  result_text TEXT NULL,
  resume_critique TEXT NULL,
  job_critique TEXT NULL,
  resume_holland_json JSON NULL,
  job_holland_json JSON NULL,
  owner_user_type VARCHAR(16) NULL,
  owner_account VARCHAR(128) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE aimap_job_catalog (
  record_id VARCHAR(32) NOT NULL PRIMARY KEY,
  title VARCHAR(256) NOT NULL,
  org VARCHAR(256) NOT NULL,
  riasec_json JSON NOT NULL,
  skills_json JSON NOT NULL
);

CREATE TABLE aimap_candidate_catalog (
  record_id VARCHAR(32) NOT NULL PRIMARY KEY,
  title VARCHAR(256) NOT NULL,
  org VARCHAR(256) NOT NULL,
  riasec_json JSON NOT NULL,
  skills_json JSON NOT NULL
);
