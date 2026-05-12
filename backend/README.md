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
   - `aimap_job_market`、`aimap_talent_pool`（Flyway V3 由职位/候选人示例目录重命名并扩展；企业 JD 进人才市场，个人自愿进人才库）

JDBC URL 形态与 MySQL 一致：`jdbc:mysql://主机:端口/aimap?...`（见 `application.yml`）。

## 2) Configure Ark API Key

**Do not commit API keys into Git.**

### Option A (recommended): `backend/.env.backend.local`

1. Copy `backend/.env.backend.local.example` to **`backend/.env.backend.local`**（已 gitignore）。
2. 在该文件中填写 **`ARK_API_KEY=你的密钥`**。`import-backend-env.ps1` 与 `start-backend.ps1` 会在加载 `.env.backend` 之后加载该文件，**后者覆盖前者**；Spring 启动时也会从该文件读取密钥（与 Docker 挂载 `backend` 目录时的行为一致）。
3. **`backend/.env.backend`** 可继续只放数据库等配置，`ARK_API_KEY` 仍可保留占位符，避免误提交真实 Key。

### Option A2: 仅使用 `backend/.env.backend`

1. Copy `backend/.env.backend.example` to `backend/.env.backend`.
2. Replace **`ARK_API_KEY=__PUT_NEW_KEY_HERE__`** with your **new** key after revoking the old one in the cloud console. While the placeholder remains, the loader **does not** override `ARK_API_KEY` from Windows User environment (so old keys keep working until you paste the new value).
3. From **`backend/`**, run **`.\start-backend.ps1`** (repo-root `.tools\apache-maven-*` is resolved automatically). It loads `.env.backend` for Ark and DB variables once set.

### 验证 Ark 是否已启用

启动后端后请求 **`GET /api/v1/llm/status`**（无需鉴权），应返回 **`"arkEnabled": true`** 与当前 **`model`** 名称；若为 `false`，请检查密钥文件路径、`user.dir` 与 Docker 环境变量。

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

### Document upload: full-text extraction

`POST /api/v1/document/upload` uses **Apache Tika** (`tika-core` + `tika-parsers-standard-package`) to read **PDF / DOC / DOCX** plain text, then sends that text (truncated) to Ark with the same JSON output contract as before. **Scan-only PDFs** (image pages, no text layer) and **password-protected** files often produce empty text; OCR is not part of this stack. Limits: `aimap.document.tika-write-limit` and `aimap.document.llm-body-max-chars` in `application.yml`.

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
- `GET /api/v1/llm/status` — Ark 是否已加载密钥（`arkEnabled`），不调用远端、不返回密钥
- `POST /api/v1/llm/extract`
- `POST /api/v1/llm/explain-match`
- `POST /api/v1/llm/generate-suggestion`
