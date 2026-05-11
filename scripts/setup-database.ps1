# One-shot: start Docker MySQL (preferred) OR print how to run SQL against existing MySQL/GreatSQL.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host '=== AImap database setup ===' -ForegroundColor Cyan

# 1) Try Docker (repo root docker-compose.yml, host port 3307)
if (Get-Command docker -ErrorAction SilentlyContinue) {
  try {
    Set-Location $root
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
      docker compose up -d
      Write-Host 'Docker Compose started. Wait ~30s then start backend (Flyway will create tables).' -ForegroundColor Green
      Write-Host "Use backend/.env.backend with DB_PORT=3307 (see .env.backend.example)." -ForegroundColor Green
      exit 0
    }
  } catch {}
}

Write-Host 'Docker daemon not running or unavailable.' -ForegroundColor Yellow
Write-Host ''
Write-Host 'Option A — Start Docker Desktop, then run again:' -ForegroundColor Cyan
Write-Host "  cd `"$root`""
Write-Host '  docker compose up -d'
Write-Host ''
Write-Host 'Option B — Use installed MySQL / GreatSQL:' -ForegroundColor Cyan
Write-Host '  1) As admin, run:' 
Write-Host "     mysql -u root -p < `"$root\database\setup-aimap.sql`""
Write-Host '  2) Edit backend/.env.backend : DB_HOST DB_PORT(usually 3306) DB_USER=aimap DB_PASSWORD=aimap_dev'
Write-Host '  3) Start backend — Flyway creates tables on first run.'
Write-Host ''
