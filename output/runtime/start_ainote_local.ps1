$ErrorActionPreference = 'Stop'

foreach ($name in @('HTTP_PROXY', 'HTTPS_PROXY', 'ALL_PROXY', 'WS_PROXY', 'WSS_PROXY')) {
    Remove-Item -Path ("Env:" + $name) -ErrorAction SilentlyContinue
}

$env:DEEPSEEK_API_KEY = 'local-validation-placeholder-not-a-key'

& 'C:\Users\Administrator\Documents\ChatGPT\AI备忘录\ainote-product\repos\api\scripts\start-local.ps1' `
    -SkipBuild `
    -EnvFile 'C:\Users\Administrator\Documents\ChatGPT\AI备忘录\ainote-coding-copy\api\.env' `
    -PublicBaseUrl 'http://192.168.31.213:8080/'
