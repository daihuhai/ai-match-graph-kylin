-- GreatSQL / MySQL 8+ compatible (utf8mb4, InnoDB)
CREATE TABLE aimap_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_type VARCHAR(16) NOT NULL,
  account VARCHAR(128) NOT NULL,
  password VARCHAR(255) NOT NULL,
  resume_riasec_json JSON NULL COMMENT 'PERSON: last resume Holland R-I-A-S-E-C',
  job_riasec_json JSON NULL COMMENT 'COMPANY: last JD Holland',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_type_account (user_type, account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE aimap_document (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  file_name VARCHAR(512) NOT NULL,
  file_type VARCHAR(16) NOT NULL,
  doc_type VARCHAR(32) NOT NULL,
  started_at_ms BIGINT NOT NULL,
  result_text MEDIUMTEXT NULL,
  resume_critique MEDIUMTEXT NULL,
  job_critique MEDIUMTEXT NULL,
  resume_holland_json JSON NULL,
  job_holland_json JSON NULL,
  owner_user_type VARCHAR(16) NULL,
  owner_account VARCHAR(128) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE aimap_job_catalog (
  record_id VARCHAR(32) NOT NULL PRIMARY KEY,
  title VARCHAR(256) NOT NULL,
  org VARCHAR(256) NOT NULL,
  riasec_json JSON NOT NULL,
  skills_json JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE aimap_candidate_catalog (
  record_id VARCHAR(32) NOT NULL PRIMARY KEY,
  title VARCHAR(256) NOT NULL,
  org VARCHAR(256) NOT NULL,
  riasec_json JSON NOT NULL,
  skills_json JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
