$ErrorActionPreference = 'Stop'

$packageRoot = Split-Path -Parent $PSScriptRoot
$siteRoot = Join-Path $packageRoot 'site'
$h5Root = Join-Path $siteRoot 'h5'
$wwwRoot = Join-Path $h5Root 'www'
$utf8 = [System.Text.UTF8Encoding]::new($false)

$marketingPages = @('home.html', 'product.html', 'support.html')
$stylePattern = [regex]::new(
    '<style(?<attr>[^>]*)>(?<text>.*?)</style>',
    [System.Text.RegularExpressions.RegexOptions]::Singleline -bor
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)

foreach ($pageName in $marketingPages) {
    $pagePath = Join-Path $wwwRoot $pageName
    $html = [System.IO.File]::ReadAllText($pagePath)

    # The archived HTML was captured with browser extensions enabled. Remove
    # extension-only style blocks while preserving the site's own embedded CSS.
    $matches = @($stylePattern.Matches($html) | Where-Object {
        $_.Groups['text'].Value -match 'jj-flash-note|chatgpt-quick-query|gpts-primary|chrome-extension://'
    } | Sort-Object Index -Descending)
    foreach ($match in $matches) {
        $html = $html.Remove($match.Index, $match.Length)
    }

    # Everything after this marker belongs to captured browser extensions.
    $extensionMarker = '<div class="xl-chrome-ext-bar_'
    $markerIndex = $html.IndexOf($extensionMarker, [System.StringComparison]::OrdinalIgnoreCase)
    if ($markerIndex -lt 0) {
        throw "Expected extension marker was not found in $pagePath"
    }

    $html = $html.Substring(0, $markerIndex)
    $html = $html.Replace('./static/', '/h5/www/static/')
    $html = $html.Replace('../app-release.apk', '/h5/app-release.apk')
    $html = $html.TrimEnd() + "`n</body>`n</html>`n"

    [System.IO.File]::WriteAllText($pagePath, $html, $utf8)
}

Copy-Item (Join-Path $wwwRoot 'home.html') (Join-Path $siteRoot 'index.html') -Force
Copy-Item (Join-Path $wwwRoot 'home.html') (Join-Path $siteRoot 'home.html') -Force
Copy-Item (Join-Path $wwwRoot 'product.html') (Join-Path $siteRoot 'product.html') -Force
Copy-Item (Join-Path $wwwRoot 'support.html') (Join-Path $siteRoot 'support.html') -Force

foreach ($fileName in @('404.html', 'download.html', 'download.txt', 'share.html', 'share.txt', 'favicon.ico')) {
    Copy-Item (Join-Path $h5Root $fileName) (Join-Path $siteRoot $fileName) -Force
}

$forbidden = @('chrome-extension://', '<chatgpt-sidebar', 'jj-flash-note', 'xl-chrome-ext-bar_')
foreach ($pageName in @('index.html', 'home.html', 'product.html', 'support.html')) {
    $pagePath = Join-Path $siteRoot $pageName
    $html = [System.IO.File]::ReadAllText($pagePath)
    foreach ($needle in $forbidden) {
        if ($html.Contains($needle)) {
            throw "Sanitization failed: $needle remains in $pagePath"
        }
    }
}

Write-Host "Recovery site prepared at $siteRoot"
