# One-shot: start Docker openGauss (preferred) OR print how to run SQL against existing openGauss.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host '=== AImap database setup (openGauss) ===' -ForegroundColor Cyan

# 1) Try Docker (repo root docker-compose.yml, host port 5432)
if (Get-Command docker -ErrorAction SilentlyContinue) {
  try {
    Set-Location $root
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
      docker compose up -d
      Write-Host 'Docker Compose started. Running ensure-dev-db.ps1 to create aimap database...' -ForegroundColor Green
      & (Join-Path $PSScriptRoot 'ensure-dev-db.ps1')
      Write-Host 'Use backend/.env.backend with DB_PORT=5432 (see .env.backend.example).' -ForegroundColor Green
      exit 0
    }
  } catch {}
}

Write-Host 'Docker daemon not running or unavailable.' -ForegroundColor Yellow
Write-Host ''
Write-Host 'Option A — Start Docker Desktop, then run again:' -ForegroundColor Cyan
Write-Host "  cd `"$root`""
Write-Host '  docker compose up -d'
Write-Host '  powershell -File .\scripts\ensure-dev-db.ps1'
Write-Host ''
Write-Host 'Option B — Use installed openGauss on Kylin / Linux:' -ForegroundColor Cyan
Write-Host '  1) As admin, run:'
Write-Host "     gsql -d postgres -U omm -W -f `"$root\database\setup-aimap.sql`""
Write-Host '  2) Edit backend/.env.backend : DB_HOST DB_PORT(5432) DB_USER=aimap DB_PASSWORD=aimap_dev'
Write-Host '  3) Start backend — Flyway creates tables on first run.'
Write-Host ''
