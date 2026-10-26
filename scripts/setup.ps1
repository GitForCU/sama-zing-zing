$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolDir = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Path $toolDir -Force | Out-Null
$nodeDir = Get-ChildItem -LiteralPath $toolDir -Directory -Filter 'node-*-win-x64' | Sort-Object Name -Descending | Select-Object -First 1
if (!$nodeDir) {
    Write-Host 'Downloading Node.js LTS from nodejs.org...'
    $sums = (Invoke-WebRequest -UseBasicParsing -Uri 'https://nodejs.org/dist/latest-v22.x/SHASUMS256.txt').Content
    $line = ($sums -split "`n" | Where-Object { $_ -match 'node-v[0-9.]+-win-x64.zip$' } | Select-Object -First 1).Trim() -split '\s+'
    $zip = Join-Path $toolDir 'node.zip'
    Invoke-WebRequest -UseBasicParsing -Uri ('https://nodejs.org/dist/latest-v22.x/' + $line[1]) -OutFile $zip
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLower() -ne $line[0]) { throw 'Node checksum mismatch' }
    Expand-Archive -LiteralPath $zip -DestinationPath $toolDir -Force
    $nodeDir = Get-ChildItem -LiteralPath $toolDir -Directory -Filter 'node-*-win-x64' | Select-Object -First 1
}
$mavenDir = Join-Path $toolDir 'apache-maven-3.9.11'
if (!(Test-Path -LiteralPath "$mavenDir\bin\mvn.cmd")) {
    Write-Host 'Downloading Maven from Maven Central...'
    $uri = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'
    $zip = Join-Path $toolDir 'maven.zip'
    Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $zip
    $expected = ((Invoke-WebRequest -UseBasicParsing -Uri ($uri + '.sha512')).Content.Trim() -split '\s+')[0]
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA512).Hash.ToLower() -ne $expected.ToLower()) { throw 'Maven checksum mismatch' }
    Expand-Archive -LiteralPath $zip -DestinationPath $toolDir -Force
}
$jdk = 'C:\Program Files\Java\jdk-21'
if (Test-Path -LiteralPath "$jdk\bin\java.exe") { $env:JAVA_HOME = $jdk }
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath "$env:JAVA_HOME\bin\javac.exe")) { throw 'Install JDK 21 and set JAVA_HOME before continuing.' }
$env:Path = $nodeDir.FullName + ';' + $env:Path
Push-Location $projectRoot
try {
    & (Join-Path $nodeDir.FullName 'npm.cmd') --prefix frontend ci --no-audit --no-fund --cache "$toolDir\npm-cache"
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
    & "$mavenDir\bin\mvn.cmd" -f backend/pom.xml "-Dmaven.repo.local=$toolDir\m2" test-compile -q
    if ($LASTEXITCODE -ne 0) { throw 'Backend compilation failed' }
    Write-Host 'SAMA is ready. Run start.cmd.'
} finally { Pop-Location }
