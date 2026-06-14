-- 在 KingbaseES 上以管理员账号（如 system）执行一次，然后启动 Spring Boot 应用。
-- 示例：ksql -d test -U system -W -f setup-aimap.sql

CREATE DATABASE aimap;

CREATE USER aimap WITH PASSWORD 'AimapDev1!';

GRANT ALL PRIVILEGES ON DATABASE aimap TO aimap;

\c aimap

GRANT ALL ON SCHEMA public TO aimap;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aimap;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aimap;
