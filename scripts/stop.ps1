$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$stateFile = Join-Path $projectRoot '.local\processes.json'
if (!(Test-Path -LiteralPath $stateFile)) { Write-Host 'No SAMA launcher state was found.';exit 0 }
$state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
if ($state.root -ne $projectRoot) { throw 'Launcher state belongs to another directory.' }
$all = @(Get-CimInstance Win32_Process)
$roots = @($state.backend,$state.frontend)
$owned = New-Object 'System.Collections.Generic.HashSet[int]'
foreach ($processId in $roots) {
    $p = $all | Where-Object ProcessId -eq $processId
    if ($p -and $p.CommandLine -like "*$PSScriptRoot\run-*.ps1*") { [void]$owned.Add([int]$processId) }
}
do {
    $added=$false
    foreach ($p in $all) { if ($owned.Contains([int]$p.ParentProcessId) -and $owned.Add([int]$p.ProcessId)) { $added=$true } }
} while ($added)
$opts = Join-Path $projectRoot '.local\postgres\postmaster.opts'
$pidFile = Join-Path $projectRoot '.local\postgres\postmaster.pid'
if ((Test-Path -LiteralPath $opts) -and (Test-Path -LiteralPath $pidFile)) {
    $options = Get-Content -LiteralPath $opts -Raw
    if ($options -match '^(?:"([^"]+postgres.exe)"|(\S+postgres.exe))') {
        $postgresExe = if ($Matches[1]) { $Matches[1] } else { $Matches[2] }
        $pgCtl = Join-Path (Split-Path -Parent $postgresExe) 'pg_ctl.exe'
        $databaseProcessId = (Get-Content -LiteralPath $pidFile -TotalCount 1).Trim()
        $databaseDirectory = Join-Path $projectRoot '.local\postgres'
        $databaseProcess = $all | Where-Object {
            $_.ProcessId -eq $databaseProcessId -and $_.Name -eq 'postgres.exe' -and
            $_.ExecutablePath -eq $postgresExe -and $_.CommandLine -like "*$databaseDirectory*"
        }
        if ($databaseProcess -and (Test-Path -LiteralPath $pgCtl)) { & $pgCtl -D $databaseDirectory -m fast -w stop }
    }
}
foreach ($processId in $owned) { Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue }
Write-Host 'SAMA stopped. Local database files were preserved.'
