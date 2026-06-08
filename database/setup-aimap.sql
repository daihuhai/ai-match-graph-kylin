-- 在 openGauss 上以管理员账号（如 omm / gaussdb）执行一次，然后启动 Spring Boot 应用。
-- 示例：gsql -d postgres -U omm -W -f setup-aimap.sql

CREATE DATABASE aimap ENCODING 'UTF8' TEMPLATE template0;

CREATE USER aimap IDENTIFIED BY 'AimapDev1!';

GRANT ALL PRIVILEGES ON DATABASE aimap TO aimap;

\c aimap

GRANT ALL ON SCHEMA public TO aimap;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO aimap;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO aimap;
