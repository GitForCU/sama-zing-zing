param([switch]$TestServer)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nodeDir = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -Filter 'node-*-win-x64' | Sort-Object Name -Descending | Select-Object -First 1
if (!$nodeDir) { throw 'Run scripts/setup.ps1 first.' }
$env:Path = $nodeDir.FullName + ';' + $env:Path
Push-Location (Join-Path $projectRoot 'frontend')
try {
    if ($TestServer) { $env:SAMA_API_URL = 'http://127.0.0.1:8081'; & (Join-Path $nodeDir.FullName 'node.exe') 'node_modules\vite\bin\vite.js' --host 127.0.0.1 --port 5174 }
    else { & (Join-Path $nodeDir.FullName 'npm.cmd') run dev }
    if ($LASTEXITCODE -ne 0) { throw "Frontend exited with code $LASTEXITCODE" }
} finally { Pop-Location }
