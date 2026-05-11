-- 在 MySQL 8 / GreatSQL 上以管理员账号执行一次（mysql -u root -p < setup-aimap.sql）
-- 创建库、应用账号及权限；应用启动时 Flyway 会自动建业务表。

CREATE DATABASE IF NOT EXISTS aimap
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'aimap'@'%' IDENTIFIED BY 'aimap_dev';
ALTER USER 'aimap'@'%' IDENTIFIED BY 'aimap_dev';

GRANT ALL PRIVILEGES ON aimap.* TO 'aimap'@'%';
FLUSH PRIVILEGES;
