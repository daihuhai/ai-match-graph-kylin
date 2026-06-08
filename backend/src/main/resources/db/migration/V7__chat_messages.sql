-- 消息中心：会话、参与者收件箱、消息正文（按参与者隔离，无全局共享会话池）
-- 仅 PERSON ↔ COMPANY 双边会话；查询必须经 aimap_chat_participant 限定当前用户

CREATE TABLE aimap_chat_thread (
  id VARCHAR(96) NOT NULL PRIMARY KEY,
  person_user_type VARCHAR(16) NOT NULL DEFAULT 'PERSON',
  person_account VARCHAR(128) NOT NULL,
  company_user_type VARCHAR(16) NOT NULL DEFAULT 'COMPANY',
  company_account VARCHAR(128) NOT NULL,
  context_record_id VARCHAR(64) NOT NULL,
  context_title VARCHAR(256) NOT NULL DEFAULT '',
  context_org VARCHAR(256) NOT NULL DEFAULT '',
  last_message_at TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_chat_thread_parties_context UNIQUE (
    person_user_type,
    person_account,
    company_user_type,
    company_account,
    context_record_id
  )
);

CREATE INDEX idx_chat_thread_person ON aimap_chat_thread (person_user_type, person_account, last_message_at);
CREATE INDEX idx_chat_thread_company ON aimap_chat_thread (company_user_type, company_account, last_message_at);

CREATE TRIGGER trg_aimap_chat_thread_updated_at
  BEFORE UPDATE ON aimap_chat_thread
  FOR EACH ROW EXECUTE PROCEDURE aimap_touch_updated_at();

CREATE TABLE aimap_chat_participant (
  id BIGSERIAL PRIMARY KEY,
  thread_id VARCHAR(96) NOT NULL,
  user_type VARCHAR(16) NOT NULL,
  account VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL DEFAULT '',
  last_read_at TIMESTAMP NULL DEFAULT NULL,
  last_read_message_id BIGINT NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_chat_participant_user UNIQUE (thread_id, user_type, account)
);

CREATE INDEX idx_chat_participant_inbox ON aimap_chat_participant (user_type, account, updated_at);
CREATE INDEX idx_chat_participant_thread ON aimap_chat_participant (thread_id);

CREATE TRIGGER trg_aimap_chat_participant_updated_at
  BEFORE UPDATE ON aimap_chat_participant
  FOR EACH ROW EXECUTE PROCEDURE aimap_touch_updated_at();

CREATE TABLE aimap_chat_message (
  id BIGSERIAL PRIMARY KEY,
  thread_id VARCHAR(96) NOT NULL,
  sender_user_type VARCHAR(16) NOT NULL,
  sender_account VARCHAR(128) NOT NULL,
  sender_name VARCHAR(128) NOT NULL DEFAULT '',
  body TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_message_thread_time ON aimap_chat_message (thread_id, created_at);
CREATE INDEX idx_chat_message_thread_id ON aimap_chat_message (thread_id, id);
