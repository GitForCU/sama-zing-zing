$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\..\database-settings.ps1"
function Assert-Equal($actual, $expected) {
    if ($actual -cne $expected) { throw 'Configuration assertion failed. Values omitted.' }
}
function Assert-Throws([scriptblock]$action) {
    $failed = $false
    try { & $action } catch { $failed = $true }
    if (!$failed) { throw 'Expected configuration to be rejected.' }
}
$keys = @('SAMA_DATABASE_MODE', 'DATABASE_URL', 'DATABASE_USER', 'DATABASE_PASSWORD', 'DATABASE_POOL_SIZE', 'SPRING_PROFILES_ACTIVE', 'COOKIE_SECURE', 'SAMA_AUTH_MODE', 'SUPABASE_URL', 'SUPABASE_PUBLISHABLE_KEY')
$original = @{}
$temporaryFile = [System.IO.Path]::GetTempFileName()
try {
    foreach ($key in $keys) {
        $original[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $null, 'Process')
    }
    Assert-Equal (Get-SamaDatabaseMode) 'local'
    Assert-Equal (Get-SamaAuthMode) 'local'
    $env:SAMA_DATABASE_MODE = 'invalid'
    Assert-Throws { Get-SamaDatabaseMode }
    $env:SAMA_DATABASE_MODE = 'supabase'
    Assert-Equal (Get-SamaAuthMode) 'supabase'
    Assert-Throws { Assert-SamaAuthConfiguration }
    $env:SUPABASE_URL = 'https://example.supabase.co'
    $env:SUPABASE_PUBLISHABLE_KEY = 'sb_publishable_example'
    Assert-SamaAuthConfiguration
    $env:SUPABASE_PUBLISHABLE_KEY = 'sb_secret_example'
    Assert-Throws { Assert-SamaAuthConfiguration }
    $env:SUPABASE_PUBLISHABLE_KEY = 'sb_publishable_example'
    Assert-Throws { Assert-SamaSupabaseConfiguration }
    $env:DATABASE_URL = 'jdbc:postgresql://example.pooler.supabase.com:5432/postgres?sslmode=require'
    $env:DATABASE_USER = 'postgres.example'
    @(
        '# Literal values must not run PowerShell or discard # and =.'
        'DATABASE_PASSWORD="literal $() # equals=value"'
        'DATABASE_POOL_SIZE=3'
    ) | Set-Content -LiteralPath $temporaryFile -Encoding UTF8
    Import-SamaEnvironment -Path $temporaryFile
    Assert-Equal $env:DATABASE_PASSWORD 'literal $() # equals=value'
    Assert-Equal $env:DATABASE_POOL_SIZE '3'
    Assert-SamaSupabaseConfiguration
    $env:DATABASE_POOL_SIZE = '5'
    Import-SamaEnvironment -Path $temporaryFile
    Assert-Equal $env:DATABASE_POOL_SIZE '5'
    $env:DATABASE_URL = 'jdbc:postgresql://example:5432/postgres'
    Assert-Throws { Assert-SamaSupabaseConfiguration }
    $env:DATABASE_URL = 'jdbc:postgresql://example:6543/postgres?sslmode=require'
    Assert-Throws { Assert-SamaSupabaseConfiguration }
    $env:DATABASE_URL = 'jdbc:postgresql://example:5432/postgres?sslmode=require&password=hidden'
    Assert-Throws { Assert-SamaSupabaseConfiguration }
    $env:DATABASE_URL = 'jdbc:postgresql://YOUR_POOLER_HOST:5432/postgres?sslmode=require'
    Assert-Throws { Assert-SamaSupabaseConfiguration }
    'DATABASE_PASSWORD="unclosed' | Set-Content -LiteralPath $temporaryFile -Encoding UTF8
    Assert-Throws { Import-SamaEnvironment -Path $temporaryFile }
    'UNSUPPORTED_KEY=value' | Set-Content -LiteralPath $temporaryFile -Encoding UTF8
    Assert-Throws { Import-SamaEnvironment -Path $temporaryFile }
    @('COOKIE_SECURE=false', 'COOKIE_SECURE=true') | Set-Content -LiteralPath $temporaryFile -Encoding UTF8
    Assert-Throws { Import-SamaEnvironment -Path $temporaryFile }
    Write-Host 'Database settings checks passed (literal secrets, overrides, mode and connection validation).'
} finally {
    foreach ($key in $original.Keys) { [Environment]::SetEnvironmentVariable($key, $original[$key], 'Process') }
    Remove-Item -LiteralPath $temporaryFile
}
