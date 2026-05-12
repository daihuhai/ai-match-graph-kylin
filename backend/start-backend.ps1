# Portable Maven under repo .tools/apache-maven-*: package then java -jar.
# On some Windows paths with non-ASCII chars, mvn spring-boot:run may fail; jar launch is more reliable.
# Loads ./.env.backend then ./.env.backend.local (ARK_API_KEY, DB_*, etc.).
$ErrorActionPreference = 'Stop'
$backendDir = $PSScriptRoot
. (Join-Path $backendDir 'import-backend-env.ps1')
$repoRoot = (Resolve-Path (Join-Path $backendDir '..')).Path
$mavenHome = Get-ChildItem -Path (Join-Path $repoRoot '.tools') -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
if (-not $mavenHome) {
  Write-Error "Maven not found: $repoRoot\.tools\apache-maven-* . Extract Maven there or use mvn from PATH, then mvn package and java -jar."
}
$mvn = Join-Path $mavenHome.FullName 'bin\mvn.cmd'
Push-Location $backendDir
try {
  & $mvn -q package -DskipTests
  $jar = Join-Path $backendDir 'target\aimap-backend-0.0.1-SNAPSHOT.jar'
  if (-not (Test-Path $jar)) { Write-Error "Jar not found after build: $jar" }
  & java -jar $jar
}
finally {
  Pop-Location
}
