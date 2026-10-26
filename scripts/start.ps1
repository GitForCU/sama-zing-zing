$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\database-settings.ps1"
Import-SamaEnvironment -Path (Join-Path $projectRoot '.env')
$databaseMode = Get-SamaDatabaseMode
if ($databaseMode -eq 'supabase') { Assert-SamaSupabaseConfiguration }
Assert-SamaAuthConfiguration
$authMode = Get-SamaAuthMode
$localDir = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $localDir -Force | Out-Null
if (!(Test-Path -LiteralPath "$projectRoot\frontend\node_modules") -or !(Test-Path -LiteralPath "$projectRoot\.tools\apache-maven-3.9.11")) { & "$PSScriptRoot\setup.ps1" }
foreach ($port in @(8080,5173)) {
    $probe = New-Object System.Net.Sockets.TcpClient
    try { $probe.Connect('127.0.0.1',$port); throw "Port $port is already in use. Stop the existing server first." }
    catch [System.Net.Sockets.SocketException] {} finally { $probe.Dispose() }
}
$backendScript = Join-Path $PSScriptRoot 'run-backend.ps1'
$frontendScript = Join-Path $PSScriptRoot 'run-frontend.ps1'
$backend = Start-Process -FilePath 'powershell.exe' -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$backendScript`"" -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput "$localDir\backend.log" -RedirectStandardError "$localDir\backend-error.log" -PassThru
$frontend = Start-Process -FilePath 'powershell.exe' -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$frontendScript`"" -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput "$localDir\frontend.log" -RedirectStandardError "$localDir\frontend-error.log" -PassThru
@{backend=$backend.Id;frontend=$frontend.Id;root=$projectRoot;startedAt=(Get-Date).ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath "$localDir\processes.json" -Encoding UTF8
Write-Host 'Starting SAMA. Logs are in .local/.'
$ready = $false
for ($attempt=0;$attempt -lt 90;$attempt++) {
    try {
        $api = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/api/auth/csrf' -TimeoutSec 2
        $web = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5173' -TimeoutSec 2
        if ($api.StatusCode -eq 200 -and $web.StatusCode -eq 200) { $ready=$true;break }
    } catch {}
    Start-Sleep -Milliseconds 1000
}
if (!$ready) { throw 'SAMA did not become ready. Inspect .local/backend.log and .local/backend-error.log, then run stop.cmd.' }
Write-Host 'Open http://127.0.0.1:5173'
Write-Host "Database mode: $databaseMode"
Write-Host "Authentication mode: $authMode"
if ($authMode -eq 'local' -and ($databaseMode -eq 'local' -or ($env:SPRING_PROFILES_ACTIVE -split ',' | ForEach-Object { $_.Trim() }) -contains 'demo')) {
    Write-Host 'Demo emails: student@sama.local / teacher@sama.local'
    Write-Host 'Demo password: SamaDemo123!'
}
Write-Host 'Run stop.cmd to stop both servers. Data is preserved.'
