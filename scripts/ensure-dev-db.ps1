# Start local MySQL 8 via Docker (see repo root docker-compose.yml). Port 3307 on host.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $root
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Write-Error "Docker not found. Install Docker Desktop or start the database manually."
}
docker compose up -d
$deadline = (Get-Date).AddSeconds(90)
do {
  $j = docker inspect --format='{{.State.Health.Status}}' aimap-mysql-dev 2>$null
  if ($j -eq 'healthy') { Write-Host "Database container is healthy."; exit 0 }
  Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
Write-Warning "Container not healthy yet; check: docker logs aimap-mysql-dev"
exit 0
