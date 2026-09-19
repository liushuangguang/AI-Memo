param(
    [switch]$SkipBuild,
    [string]$EnvFile,
    [string]$PublicBaseUrl = 'http://127.0.0.1:8080/',
    [string]$AiBaseUrl,
    [string]$AiModel,
    [switch]$DirectAi,
    [string]$RuntimeDirectory
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if (!$RuntimeDirectory) { $RuntimeDirectory = $repoRoot }
$RuntimeDirectory = (Resolve-Path -LiteralPath $RuntimeDirectory).Path
$jarPath = Join-Path $repoRoot 'note/target/note-0.0.1-SNAPSHOT.jar'
$originalProcessEnvironment = @{}

function Set-TemporaryProcessEnvironment {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [AllowNull()][string]$Value
    )

    if (-not $originalProcessEnvironment.ContainsKey($Name)) {
        $originalProcessEnvironment[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process')
    }
    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

function Restore-ProcessEnvironment {
    foreach ($entry in $originalProcessEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
}

function Import-DotEnvForCurrentProcess {
    param([Parameter(Mandatory = $true)][string]$Path)

    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    foreach ($line in [System.IO.File]::ReadLines($resolvedPath)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) {
            continue
        }
        if ($line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            throw "Invalid .env entry (value not displayed)"
        }

        $name = $matches[1]
        $value = $matches[2].Trim()
        if ($value.Length -ge 2) {
            $first = $value[0]
            $last = $value[$value.Length - 1]
            if (($first -eq '"' -and $last -eq '"') -or
                ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        Set-TemporaryProcessEnvironment -Name $name -Value $value
    }
}

function Read-RequiredSecretForCurrentProcess {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Prompt
    )

    if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name, 'Process'))) {
        return
    }

    $secureValue = Read-Host -Prompt $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        $plainValue = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        if ([string]::IsNullOrWhiteSpace($plainValue)) {
            throw "$Name is required"
        }
        Set-TemporaryProcessEnvironment -Name $Name -Value $plainValue
    } finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
        $plainValue = $null
    }
}

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & (Join-Path $repoRoot 'mvnw.cmd') '-pl' 'note' '-am' 'package' '-DskipTests'
        $buildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($buildExitCode -ne 0) {
        throw "Maven build failed with exit code $buildExitCode"
    }
}

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "JAR not found: $jarPath. Run without -SkipBuild first."
}

$startedProcess = $null
try {
    if (-not [string]::IsNullOrWhiteSpace($EnvFile)) {
        Import-DotEnvForCurrentProcess -Path $EnvFile
    }

    if ($AiBaseUrl) {
        $parsedAiBase = [Uri]$AiBaseUrl
        if (!$parsedAiBase.IsAbsoluteUri -or $parsedAiBase.Scheme -ne 'https' -or $parsedAiBase.UserInfo -or $parsedAiBase.Query -or $parsedAiBase.Fragment) {
            throw 'AI provider base URL must be HTTPS with no embedded credentials/query'
        }
        Set-TemporaryProcessEnvironment -Name 'AI_CHAT_BASE_URL' -Value $AiBaseUrl.TrimEnd('/')
    }
    if ($AiModel) { Set-TemporaryProcessEnvironment -Name 'AI_CHAT_MODEL' -Value $AiModel.Trim() }
    if ($DirectAi) { Set-TemporaryProcessEnvironment -Name 'AI_CHAT_DIRECT' -Value 'true' }
    if (![string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable('AI_CHAT_BASE_URL', 'Process'))) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable('AI_CHAT_MODEL', 'Process'))) { throw 'An explicit AI_CHAT_MODEL is required for an independent provider' }
        Read-RequiredSecretForCurrentProcess -Name 'AI_CHAT_API_KEY' -Prompt 'Independent AI provider key (Java process memory only)'
    } else {
        Read-RequiredSecretForCurrentProcess -Name 'DEEPSEEK_API_KEY' -Prompt 'DeepSeek API key (Java process memory only)'
    }

    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable('DEEPSEEK_MODEL', 'Process'))) {
        Set-TemporaryProcessEnvironment -Name 'DEEPSEEK_MODEL' -Value 'deepseek-chat'
    }
    $normalizedPublicBaseUrl = $PublicBaseUrl.Trim()
    if (-not $normalizedPublicBaseUrl.EndsWith('/')) {
        $normalizedPublicBaseUrl += '/'
    }
    Set-TemporaryProcessEnvironment -Name 'NOTE_HOST_NAME' -Value $normalizedPublicBaseUrl
    Set-TemporaryProcessEnvironment -Name 'NOTE_HOSTNAME' -Value $normalizedPublicBaseUrl

    $quotedJarPath = '"' + $jarPath + '"'
    $startedProcess = Start-Process -FilePath 'java' -ArgumentList @(
        '-jar', $quotedJarPath,
        '--spring.profiles.active=local-dev'
    ) -WorkingDirectory $RuntimeDirectory -WindowStyle Hidden -PassThru
} finally {
    Restore-ProcessEnvironment
}

$startedProcess
