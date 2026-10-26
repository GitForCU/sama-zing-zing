param([switch]$Ephemeral)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\database-settings.ps1"
if ($Ephemeral) {
    # Tests always use their isolated database, even if .env selects Supabase.
    $databaseMode = 'test'
    $env:SPRING_PROFILES_ACTIVE = 'demo'
    $env:SAMA_AUTH_MODE = 'local'
} else {
    Import-SamaEnvironment -Path (Join-Path $projectRoot '.env')
    $databaseMode = Get-SamaDatabaseMode
    Assert-SamaAuthConfiguration
    if ($databaseMode -eq 'supabase') {
        Assert-SamaSupabaseConfiguration
        $profiles = @($env:SPRING_PROFILES_ACTIVE -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $env:SPRING_PROFILES_ACTIVE = (@($profiles + 'supabase') | Select-Object -Unique) -join ','
    }
}
$jdk = 'C:\Program Files\Java\jdk-21'
if (Test-Path -LiteralPath "$jdk\bin\java.exe") { $env:JAVA_HOME = $jdk }
$maven = Join-Path $projectRoot '.tools\apache-maven-3.9.11\bin\mvn.cmd'
if (!(Test-Path -LiteralPath $maven)) { throw 'Run scripts/setup.ps1 first, or install Maven and edit this launcher.' }
$main = switch ($databaseMode) {
    'test' { 'th.ac.sama.E2eApplication' }
    'supabase' { 'th.ac.sama.SamaApplication' }
    default { 'th.ac.sama.LocalApplication' }
}
Write-Host "Backend database mode: $databaseMode"
Push-Location (Join-Path $projectRoot 'backend')
try { & $maven "-Dmaven.repo.local=$projectRoot\.tools\m2" test-compile exec:java "-Dexec.mainClass=$main" '-Dexec.classpathScope=test' '-Dexec.cleanupDaemonThreads=false' -q; if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" } }
finally { Pop-Location }
