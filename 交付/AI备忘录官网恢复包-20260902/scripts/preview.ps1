$ErrorActionPreference = 'Stop'

$packageRoot = Split-Path -Parent $PSScriptRoot
$siteRoot = Join-Path $packageRoot 'site'

Write-Host 'Local preview: http://127.0.0.1:8766/'
python -m http.server 8766 --bind 127.0.0.1 --directory $siteRoot
