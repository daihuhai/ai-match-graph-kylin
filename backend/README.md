# AImap Backend (Spring Boot)

## 1) Prerequisites
- JDK 17+
- Maven 3.9+
- **GreatSQL**（或与 MySQL 8 协议兼容的实例）用于持久化；表结构由 **Flyway** 自动创建。

## 1.5) 数据库：建库与连接

### 方式 A：Docker 本地开发（推荐，协议与 GreatSQL 兼容）

1. 安装并启动 **Docker Desktop**，在**仓库根目录**执行：`docker compose up -d`（`docker-compose.yml` 将 MySQL 8 映射到本机 **3307**，避免占用已有 **3306** 实例）。
2. 复制 `backend/.env.backend.example` 为 `backend/.env.backend`（或保留已有文件），确保含：`DB_HOST=127.0.0.1`、`DB_PORT=3307`、`DB_USER=aimap`、`DB_PASSWORD=aimap_dev`。
3. 启动后端后 Flyway 自动建表；亦可先运行 `powershell -File .\scripts\setup-database.ps1` 尝试拉起容器。

### 方式 B：本机 GreatSQL / MySQL

1. 使用具备权限的账号执行仓库根目录 **`database/setup-aimap.sql`**（创建库 `aimap` 与用户 `aimap` / `aimap_dev`），或仅执行：  
   `backend/src/main/resources/db/install-greatsql.sql`（仅建库）并自行创建同名账号与授权。
2. 在 **`backend/.env.backend`** 中设置实际 `DB_HOST`、`DB_PORT`（多为 `3306`）、`DB_USER`、`DB_PASSWORD`。
4. 启动应用后 **Flyway** 会执行 `classpath:db/migration` 下的脚本，创建并填充：
   - `aimap_user`（含 demo / admin 种子用户）
   - `aimap_document`（文档解析任务）
   - `aimap_job_catalog`、`aimap_candidate_catalog`（职位与候选人示例库 + RIASEC JSON）

JDBC URL 形态与 MySQL 一致：`jdbc:mysql://主机:端口/aimap?...`（见 `application.yml`）。

## 2) Configure Ark API Key

**Do not commit API keys into Git.**

### Option A (recommended on this repo): `backend/.env.backend`

1. Copy `backend/.env.backend.example` to `backend/.env.backend`.
2. Replace **`ARK_API_KEY=__PUT_NEW_KEY_HERE__`** with your **new** key after revoking the old one in the cloud console. While the placeholder remains, the loader **does not** override `ARK_API_KEY` from Windows User environment (so old keys keep working until you paste the new value).
3. From **`backend/`**, run **`.\start-backend.ps1`** (repo-root `.tools\apache-maven-*` is resolved automatically). It loads `.env.backend` for Ark and DB variables once set.

### Option B: session environment

PowerShell:

```powershell
$env:ARK_API_KEY="your-api-key"
```

Optional:

```powershell
$env:ARK_MODEL="doubao-seed-2-0-lite-260428"
$env:ARK_BASE_URL="https://ark.cn-beijing.volces.com/api/v3"
```

If a key was ever pasted into chat or committed, **rotate it** in the Volcengine / Ark console.

Dependency: `com.volcengine:volcengine-java-sdk-ark-runtime` (see `pom.xml`, pinned version).

## 3) Run

已安装 Maven 且路径为纯英文时：

```powershell
mvn spring-boot:run
```

**Windows 且项目路径含中文**：部分环境下 `spring-boot:run` 会报主类 `ClassNotFoundException`，可改为打包后启动：

```powershell
mvn package -DskipTests
java -jar .\target\aimap-backend-0.0.1-SNAPSHOT.jar
```

若已将 Maven 解压到仓库根目录的 `.tools\apache-maven-*`，也可一键：

```powershell
.\start-backend.ps1
```

Service starts at `http://localhost:8080`.

## 4) Implemented APIs
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/document/upload`
- `GET /api/v1/document/{id}/status`
- `GET /api/v1/document/{id}/result`
- `GET /api/v1/graph/person/{id}`
- `GET /api/v1/graph/job/{id}`
- `POST /api/v1/graph/expand`
- `POST /api/v1/match/recommend-jobs` — body optional JSON `{ "minScore": 60 }` (0–100, Holland similarity filter)
- `POST /api/v1/match/recommend-candidates` — same `{ "minScore": 60 }`
- `GET /api/v1/match/{id}/detail`
- `POST /api/v1/llm/extract`
- `POST /api/v1/llm/explain-match`
- `POST /api/v1/llm/generate-suggestion`
