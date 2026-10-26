function Import-SamaEnvironment {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (!(Test-Path -LiteralPath $Path)) { return }
    $allowed = @('SAMA_DATABASE_MODE', 'DATABASE_URL', 'DATABASE_USER', 'DATABASE_PASSWORD', 'DATABASE_POOL_SIZE', 'SPRING_PROFILES_ACTIVE', 'COOKIE_SECURE', 'SAMA_AUTH_MODE', 'SUPABASE_URL', 'SUPABASE_PUBLISHABLE_KEY')
    $settings = @{}
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $lineNumber++
        $entry = $line.Trim()
        if (!$entry -or $entry.StartsWith('#')) { continue }
        if ($entry -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') { throw "Invalid .env syntax on line $lineNumber. Use KEY=value." }
        $key = $Matches[1]
        $value = $Matches[2].Trim()
        if ($allowed -notcontains $key) { throw "Unsupported .env key on line $lineNumber. See .env.supabase.example." }
        if ($settings.ContainsKey($key)) { throw "Duplicate .env key on line $lineNumber." }
        if ($value.StartsWith('"') -or $value.StartsWith("'")) {
            if ($value.Length -lt 2 -or $value[$value.Length - 1] -ne $value[0]) { throw "Unclosed quote in .env on line $lineNumber." }
            $value = $value.Substring(1, $value.Length - 2)
        }
        # Parse as literal text. Never execute .env contents or expand $variables.
        $settings[$key] = $value
    }
    foreach ($key in $settings.Keys) {
        # Explicit process environment takes precedence over the file.
        if ($null -eq [Environment]::GetEnvironmentVariable($key, 'Process')) {
            [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
        }
    }
}

function Get-SamaDatabaseMode {
    $mode = $env:SAMA_DATABASE_MODE
    if ([string]::IsNullOrWhiteSpace($mode)) { return 'local' }
    $mode = $mode.Trim().ToLowerInvariant()
    if ($mode -notin @('local', 'supabase')) { throw 'SAMA_DATABASE_MODE must be local or supabase.' }
    return $mode
}

function Assert-SamaSupabaseConfiguration {
    foreach ($key in @('DATABASE_URL', 'DATABASE_USER', 'DATABASE_PASSWORD')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ([string]::IsNullOrWhiteSpace($value) -or $value -match 'YOUR_|CHANGE_ME|PROJECT_REF') {
            throw "Set $key in .env using your Supabase connection details. Values are not printed."
        }
    }
    if ($env:DATABASE_URL -notmatch '^jdbc:postgresql://[^\s/?#@]+/[^\s?#]+\?[^\s#]+$') {
        throw 'DATABASE_URL must be a PostgreSQL JDBC URL. Put the username/password in their separate variables.'
    }
    if ($env:DATABASE_URL -match '[?&](user|password)=') { throw 'Keep database credentials out of DATABASE_URL. Use DATABASE_USER and DATABASE_PASSWORD.' }
    if ($env:DATABASE_URL -notmatch '[?&]sslmode=(require|verify-ca|verify-full)(&|$)') { throw 'Supabase requires sslmode=require or a verifying SSL mode in DATABASE_URL.' }
    if ($env:DATABASE_URL -match ':6543/') { throw 'Use the Session pooler on port 5432 or a direct connection, not transaction pooling on 6543.' }
    if ($env:DATABASE_POOL_SIZE -and ($env:DATABASE_POOL_SIZE -notmatch '^([1-9]|10)$')) { throw 'DATABASE_POOL_SIZE must be an integer from 1 to 10.' }
}

function Get-SamaAuthMode {
    $mode = $env:SAMA_AUTH_MODE
    if ([string]::IsNullOrWhiteSpace($mode)) {
        if ((Get-SamaDatabaseMode) -eq 'supabase') { return 'supabase' }
        return 'local'
    }
    $mode = $mode.Trim().ToLowerInvariant()
    if ($mode -notin @('local', 'supabase')) { throw 'SAMA_AUTH_MODE must be local or supabase.' }
    return $mode
}

function Assert-SamaAuthConfiguration {
    if ((Get-SamaAuthMode) -ne 'supabase') { return }
    foreach ($key in @('SUPABASE_URL', 'SUPABASE_PUBLISHABLE_KEY')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ([string]::IsNullOrWhiteSpace($value) -or $value -match 'YOUR_|CHANGE_ME|PROJECT_REF') {
            throw "Set $key in .env for Supabase Auth. Never use a secret/service_role key."
        }
    }
    if ($env:SUPABASE_URL -notmatch '^https://[^/?#@\s]+/?$') { throw 'SUPABASE_URL must be the HTTPS project URL without /auth/v1.' }
    if ($env:SUPABASE_PUBLISHABLE_KEY.StartsWith('sb_secret_')) { throw 'Use the publishable key, never a Supabase secret key.' }
}
