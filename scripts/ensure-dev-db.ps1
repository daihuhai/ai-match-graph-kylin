# Start local openGauss via Docker (see repo root docker-compose.yml). Port 5432 on host.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $root
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Write-Error "Docker not found. Install Docker Desktop or start openGauss manually."
}
docker compose up -d

$deadline = (Get-Date).AddSeconds(120)
do {
  $j = docker inspect --format='{{.State.Health.Status}}' aimap-opengauss-dev 2>$null
  if ($j -eq 'healthy') { break }
  Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

if ($j -ne 'healthy') {
  Write-Warning "Container not healthy yet; check: docker logs aimap-opengauss-dev"
  exit 0
}

Write-Host "Database container is healthy. Ensuring database/user aimap ..." -ForegroundColor Green

# gsql must run as omm with openGauss env vars (see docker-compose healthcheck).
$gsqlBase = @(
  'exec', '-u', 'omm', 'aimap-opengauss-dev',
  'env', 'GAUSSHOME=/usr/local/opengauss',
  'PATH=/usr/local/opengauss/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
  'LD_LIBRARY_PATH=/usr/local/opengauss/lib', 'gsql'
)

$dbExists = & docker @gsqlBase -d postgres -p 5432 -t -A -c "SELECT 1 FROM pg_database WHERE datname = 'aimap';" 2>$null
$userExists = & docker @gsqlBase -d postgres -p 5432 -t -A -c "SELECT 1 FROM pg_roles WHERE rolname = 'aimap';" 2>$null
if ($dbExists -notmatch '1' -or $userExists -notmatch '1') {
  & docker @gsqlBase -d postgres -p 5432 -c "CREATE DATABASE aimap ENCODING 'UTF8' TEMPLATE template0;" | Out-Null
  & docker @gsqlBase -d postgres -p 5432 -c "CREATE USER aimap IDENTIFIED BY 'AimapDev1!';" 2>$null | Out-Null
  & docker @gsqlBase -d postgres -p 5432 -c "GRANT ALL PRIVILEGES ON DATABASE aimap TO aimap;" | Out-Null
  & docker @gsqlBase -d aimap -p 5432 -c "GRANT ALL ON SCHEMA public TO aimap;" | Out-Null
  # Flyway parses "$user" in search_path and issues SET ROLE; openGauss denies that for app users.
  & docker @gsqlBase -d postgres -p 5432 -c "ALTER USER aimap SET search_path TO public;" | Out-Null
  & docker @gsqlBase -d postgres -p 5432 -c "ALTER USER aimap SYSADMIN;" | Out-Null
  # Flyway (PostgreSQL driver) runs RESET ROLE; aimap must be allowed to assume omm in local dev.
  & docker @gsqlBase -d postgres -p 5432 -c "GRANT omm TO aimap;" 2>$null | Out-Null
  Write-Host "Created database aimap and user aimap." -ForegroundColor Green
} else {
  Write-Host "Database aimap already exists." -ForegroundColor Green
  & docker @gsqlBase -d postgres -p 5432 -c "ALTER USER aimap SET search_path TO public;" 2>$null | Out-Null
  & docker @gsqlBase -d postgres -p 5432 -c "ALTER USER aimap SYSADMIN;" 2>$null | Out-Null
}

exit 0
