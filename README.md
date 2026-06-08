# AI 智能匹配与能力图谱系统

仓库按职责拆分为 **文档**、**Web 前端**、**Spring Boot 后端** 三个目录，根目录仅保留本说明与跨项目资源（如 `.tools` 便携 Maven）。

| 目录 | 说明 |
|------|------|
| **`docs/`** | 交付与设计文档（产品说明书、前端架构设计方案等） |
| **`frontend/`** | Vue 3 + Vite + TypeScript 前端 |
| **`backend/`** | Spring Boot 后端（REST、`Flyway` 迁移、**openGauss** 数据源） |

快速开始：

1. **数据库（二选一）**  
   - **Docker（推荐）**：安装并启动 **Docker Desktop**，在仓库根目录执行 `docker compose up -d`（openGauss 映射到本机 **5432**），再运行 `powershell -File .\scripts\ensure-dev-db.ps1` 创建库与用户。使用 `backend/.env.backend.example` 中的默认 `DB_PORT=5432`、`DB_USER=aimap`、`DB_PASSWORD=aimap_dev`。也可运行 `powershell -File .\scripts\setup-database.ps1` 一键完成。  
   - **本机 openGauss（银河麒麟）**：用管理员账号执行 `database/setup-aimap.sql`，再在 `backend/.env.backend` 中设置实际 `DB_HOST`、`DB_PORT`（默认 5432）、`DB_USER`/`DB_PASSWORD`。  
   - 仅建空库时亦可执行 `backend/src/main/resources/db/install-opengauss.sql`，仍需创建应用账号。
2. **后端**：进入 `backend/`，编辑 `.env.backend`（已从示例复制则含数据库项），填写 `ARK_API_KEY`；Windows 可运行 `.\start-backend.ps1`。首次启动 **Flyway** 会自动建表并写入种子数据。
3. **前端**：进入 `frontend/`，执行 `npm install`、`npm run dev`（开发代理默认将 `/api` 转到 `http://127.0.0.1:8080`）。

旧路径 **`B2-前端/`** 已拆分撤销；请勿在新文档中引用。
