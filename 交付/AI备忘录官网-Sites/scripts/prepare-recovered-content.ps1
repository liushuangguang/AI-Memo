$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Resolve-Path (Join-Path $projectRoot '..\AI备忘录官网恢复包-20260902\site')
$contentRoot = Join-Path $projectRoot 'content'

New-Item -ItemType Directory -Force -Path $contentRoot | Out-Null

foreach ($fileName in @('home.html', 'product.html', 'support.html')) {
  $sourcePath = Join-Path $sourceRoot $fileName
  $targetPath = Join-Path $contentRoot $fileName
  $html = [System.IO.File]::ReadAllText($sourcePath)

  $html = $html.Replace('lang="en"', 'lang="zh-CN"')
  $html = $html.Replace('href="./home.html"', 'href="/home"')
  $html = $html.Replace('href="./product.html"', 'href="/products"')
  $html = $html.Replace('href="./support.html"', 'href="/support"')
  $html = $html.Replace(
    "window.open('/h5/app-release.apk', '__blank')",
    "window.location.href='/download'"
  )
  $html = $html.Replace(
    'https://cdn.wegic.ai/assets/onepage/thread/thumbnail/1804815425448288258/1719159004862.png',
    'https://ainote.tabtotask.top/h5/www/static/images/home1.jpg'
  )
  $html = [Regex]::Replace(
    $html,
    '<meta property="og:title"[^>]*>',
    '<meta property="og:title" content="AI备忘录">'
  )
  $html = [Regex]::Replace(
    $html,
    '<meta property="og:description"[^>]*>',
    '<meta property="og:description" content="AI备忘录支持智能记录、AI语音讨论和一键整理。">'
  )

  if ($html -notmatch 'property="og:image"') {
    $socialTags = @'
  <meta property="og:image" content="https://ainote.tabtotask.top/h5/www/static/images/home1.jpg">
  <meta name="twitter:image" content="https://ainote.tabtotask.top/h5/www/static/images/home1.jpg">
</head>
'@
    $html = $html.Replace('</head>', $socialTags)
  }

  [System.IO.File]::WriteAllText(
    $targetPath,
    $html,
    [System.Text.UTF8Encoding]::new($false)
  )
}

Copy-Item -LiteralPath (Join-Path $projectRoot 'public\h5\www\static\favicon.ico') `
  -Destination (Join-Path $projectRoot 'public\favicon.ico') -Force

foreach ($obsoleteFile in @('public\pages\home.html', 'public\favicon.svg')) {
  $obsoletePath = Join-Path $projectRoot $obsoleteFile
  if (Test-Path -LiteralPath $obsoletePath) {
    Remove-Item -LiteralPath $obsoletePath -Force
  }
}

$obsoleteDirectory = Join-Path $projectRoot 'public\pages'
if ((Test-Path -LiteralPath $obsoleteDirectory) -and
    -not (Get-ChildItem -LiteralPath $obsoleteDirectory -Force)) {
  Remove-Item -LiteralPath $obsoleteDirectory -Force
}
