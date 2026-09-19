param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$guest = 'repair-smoke-' + [Guid]::NewGuid().ToString('N')
$headers = @{ Authorization = "Guest $guest"; 'Device-Id' = $guest }
function Invoke-NotePost([string]$path, $body) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest -Method Post -Uri "$BaseUrl$path" -NoProxy -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ($body | ConvertTo-Json -Depth 12 -Compress) -TimeoutSec 205 -SkipHttpErrorCheck
    $payload = $response.Content | ConvertFrom-Json
    Write-OutputToHost -path $path -status $response.StatusCode -code $payload.code -seconds $timer.Elapsed.TotalSeconds
    if ($response.StatusCode -ne 200 -or $payload.code -ne 200) { throw "Synthetic request failed at $path (HTTP $($response.StatusCode), code $($payload.code))" }
    return $payload.data
}
function Write-OutputToHost($path,$status,$code,$seconds) {
    Write-Host ("{0} HTTP={1} business={2} seconds={3:N2}" -f $path,$status,$code,$seconds)
}
$text = "合成测试群聊。小林：2026年9月10日下午3点进行产品评审，带好演示材料。小王：本次团队购买测试手机共花费3500元，已付款。小李：周末准备看电影《流浪地球》，这是电影计划。以上均为测试样例，不是真实个人数据。"
$delta = @(@{insert=($text + "`n")}) | ConvertTo-Json -Compress
$note = Invoke-NotePost '/v2/note/create' @{title='分类与整理合成测试';content=$delta;noteType=0}
Write-Host ("stored content length={0}" -f ([string]$note.content).Length)
$validated = Invoke-NotePost '/v2/note/analysis/validateNote' @{id=$note.id}
if (!$validated.isMeaningful -and !$validated.meaningful) { throw 'Synthetic meaningful input rejected' }
$organized = Invoke-NotePost '/v2/note/analysis/organizeNote' @{recordId=$validated.recordId}
if ([string]::IsNullOrWhiteSpace($organized.bodyText)) { throw 'No organized body returned' }
Write-Host ("organized body chars={0}, tags={1}" -f ([string]$organized.bodyText).Length,@($organized.tag).Count)
$classified = @(Invoke-NotePost '/v2/note/analysis/categorizedNote' @{recordId=$validated.recordId})
Write-Host ("classified groups={0}, item counts={1}" -f $classified.Count, ((@($classified | ForEach-Object { @($_.categorizedNoteModule.items).Count })) -join ','))
$assist = Invoke-NotePost '/note/assist/validateNote' @{id=$note.id}
$directions = Invoke-NotePost '/note/assist/selectedAssistantDirection' @{noteId=$assist.recordId}
Write-Host ("recommended direction count={0}" -f @($directions.availableAssistantDirection).Count)
$rewritten = Invoke-NotePost '/note/assist/rewriteContent' @{recordId=$assist.recordId;assistDirections=@('简单优化表达');selectedContent=$null}
if ([string]::IsNullOrWhiteSpace($rewritten.rewrittenContent)) { throw 'No rewritten body returned' }
Write-Host ("rewritten chars={0}; synthetic note retained under isolated guest, no user note touched" -f ([string]$rewritten.rewrittenContent).Length)
