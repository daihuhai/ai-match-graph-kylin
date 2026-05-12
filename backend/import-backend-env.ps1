# Loads backend/.env.backend then optional .env.backend.local (local overrides; UTF-8 KEY=value).
# ARK_API_KEY: placeholder __PUT_NEW_KEY_HERE__ / your-api-key in a file is skipped so Windows
# User-level ARK_API_KEY can still apply when using start-backend.ps1.
param(
  [string] $EnvFile = (Join-Path $PSScriptRoot '.env.backend'),
  [string] $LocalEnvFile = (Join-Path $PSScriptRoot '.env.backend.local')
)

function Import-BackendEnvFile {
  param([string] $Path)
  if (-not (Test-Path -LiteralPath $Path)) {
    return
  }
  Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
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
}

Import-BackendEnvFile -Path $EnvFile
Import-BackendEnvFile -Path $LocalEnvFile
