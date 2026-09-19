param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$guest = 'video-smoke-' + [Guid]::NewGuid().ToString('N')
$headers = @{ Authorization = "Guest $guest"; 'Device-Id' = $guest }
# Only synthetic notes in a new isolated guest. Never print content or credentials.
function Post($path, $body, [switch]$Raw) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest -Method Post -Uri "$BaseUrl$path" -NoProxy -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ($body | ConvertTo-Json -Depth 12 -Compress) -TimeoutSec 205 -SkipHttpErrorCheck
    if ($response.StatusCode -ne 200) { throw "HTTP $($response.StatusCode)" }
    if ($Raw) {
        if (!$response.Content.Contains('data:')) { throw 'Missing SSE events' }
        Write-Host "$path PASS SSE chars=$($response.Content.Length) seconds=$([Math]::Round($timer.Elapsed.TotalSeconds,2))"
        return
    }
    $payload = $response.Content | ConvertFrom-Json
    if ($payload.PSObject.Properties.Name -contains 'code') {
        if ($payload.code -ne 200) { throw "business=$($payload.code)" }
        $payload = $payload.data
    }
    Write-Host "$path PASS seconds=$([Math]::Round($timer.Elapsed.TotalSeconds,2))"
    return ,$payload
}
function Probe($name, [scriptblock]$test) {
    try { & $test } catch { Write-Host "$name FAIL type=$($_.Exception.GetType().Name) reason=$($_.Exception.Message)" }
}
$noteIds = @()
foreach ($text in @('合成旅行测试：十月计划去云南大理，预算5000元。需要轻便相机记录风景。', '合成旅行测试：洱海骑行和古城散步是重点，住宿选安静的客栈。', '合成旅行测试：整理出行清单，带雨衣、防晒和充电宝，提前预订车票。')) {
    $note = Post '/v2/note/create' @{title='视频核对合成样例';content=(@(@{insert=($text + "`n")}) | ConvertTo-Json -Compress);noteType=0}
    $noteIds += $note.id
}
$validated = Post '/v2/note/analysis/validateNote' @{id=$noteIds[0]}
$record = @{recordId=$validated.recordId}
$organized = Post '/v2/note/analysis/organizeNote' $record
if ([string]::IsNullOrWhiteSpace($organized.bodyText)) { throw 'No organized body' }
Probe 'completeInfo' { $items = Post '/note/improve/completeInfo' @{content='下周找小王谈一下项目，之后再去那里见客户。'}; Write-Host "completion items=$(@($items).Count)" }
Probe 'suggestion' { Post '/v2/note/analysis/aiSuggestion' $record -Raw }
Probe 'relatedTitle' { $items = Post '/v2/note/analysis/relatedTitle' $record; Write-Host "titles=$(@($items).Count)" }
Probe 'relatedInfo' { $info = Post '/v2/note/analysis/relatedInfo?textContent=%E5%A4%A7%E7%90%86%E6%97%85%E8%A1%8C%E6%B3%A8%E6%84%8F%E4%BA%8B%E9%A1%B9' @{}; if ([string]::IsNullOrWhiteSpace($info)) {throw 'Empty detail'} }
Probe 'relatedLink' { $links = Post '/v2/note/analysis/relatedLink' $record; Write-Host "links=$(@($links).Count)" }
Probe 'illustration' { $url = Post '/v2/note/analysis/aiIllustration' $record; if ($url -notmatch '^https?://') { throw 'Missing image URL' }; $image = Invoke-WebRequest -Uri $url -NoProxy -TimeoutSec 30 -SkipHttpErrorCheck; Write-Host "image GET=$($image.StatusCode) bytes=$($image.RawContentLength)" }
Probe 'relatedNotes' { $items = Post '/v2/note/analysis/relatedNotes' $record; Write-Host "related notes=$(@($items).Count)" }
Probe 'reflection' { $result = Post '/v2/note/analysis/reflection' $record; if ([string]::IsNullOrWhiteSpace($result.summary)) {throw 'Empty reflection'}; Write-Host "reflection sources=$(@($result.sources).Count)" }
Probe 'products' { $items = Post '/v2/note/analysis/productRecommendations' $record; Write-Host "products=$(@($items).Count)" }
Probe 'theme merge' {
    $theme = Post '/note/theme/create' @{theme='大理旅行计划';description='合成数据：合并云南旅行准备和行程安排'}
    $candidates = Post "/note/theme/$($theme.id)/candidates" @{}
    Write-Host "theme candidates=$(@($candidates).Count)"
    $merged = Post "/note/theme/$($theme.id)/merge" @{sourceNoteIds=$noteIds;selectionConfirmed=$true;idempotencyKey=('smoke-' + [Guid]::NewGuid().ToString('N'))}
    Write-Host "merge result fields=$($merged.PSObject.Properties.Name -join ',')"
}
Write-Host 'Finished synthetic-only probes; user notes untouched.'
