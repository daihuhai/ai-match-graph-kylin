# Loads backend/.env.backend into current process (overrides User-level ARK_* for this session).
# UTF-8, KEY=value lines; # comments; blank lines ignored.
param(
  [string] $EnvFile = (Join-Path $PSScriptRoot '.env.backend')
)
if (-not (Test-Path -LiteralPath $EnvFile)) {
  return
}
Get-Content -LiteralPath $EnvFile -Encoding UTF8 | ForEach-Object {
  $line = $_.Trim()
  if ($line -eq '' -or $line.StartsWith('#')) {
    return
  }
  $eq = $line.IndexOf('=')
  if ($eq -lt 1) {
    return
  }
  $name = $line.Substring(0, $eq).Trim()
  $val = $line.Substring($eq + 1).Trim()
  if ($val.StartsWith('"') -and $val.EndsWith('"') -and $val.Length -ge 2) {
    $val = $val.Substring(1, $val.Length - 2)
  }
  if ($name -eq 'ARK_API_KEY') {
    $skip = [string]::IsNullOrWhiteSpace($val) -or $val -eq '__PUT_NEW_KEY_HERE__' -or $val -eq 'your-api-key'
    if ($skip) {
      return
    }
  }
  Set-Item -Path "Env:$name" -Value $val
}
