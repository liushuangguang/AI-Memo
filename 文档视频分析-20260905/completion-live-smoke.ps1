param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$owner = 'completion-smoke-20260906'
$headers = @{ Authorization = "Guest $owner"; 'Device-Id' = $owner }
function Post-Json($path, $body) {
    $result = Invoke-RestMethod -Method Post -Uri "$BaseUrl$path" -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ($body | ConvertTo-Json -Depth 20 -Compress) -TimeoutSec 180
    if ($result.code -ne 200) { throw "Business failure $path : $($result.code) $($result.msg)" }
    return $result.data
}
function Assert-True($test, $message) { if (!$test) { throw $message } }
$note = Post-Json '/v2/note/create' @{title='最终保存验证';content='原始正文：周六准备演示稿。';noteType=0}
$save = @{noteId=$note.id; summary='测试总结：先核对演示材料，再安排讨论。';saveRequestId='final-summary-20260906'}
$first = Post-Json '/v2/voice-discussion/summary/save' $save
$second = Post-Json '/v2/voice-discussion/summary/save' $save
Assert-True ($second.alreadySaved -eq $true) 'Summary retry did not deduplicate'
Assert-True ($first.note.content -eq $second.note.content) 'Summary retry changed body'
$save.summary = '不同的总结内容'
$conflict = Invoke-WebRequest -Method Post -Uri "$BaseUrl/v2/voice-discussion/summary/save" -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ($save | ConvertTo-Json) -SkipHttpErrorCheck
Assert-True ($conflict.StatusCode -eq 409) 'Changed summary payload must conflict'
$module = @{id=$note.id;module=@{noteModuleType='AI_PICTURE';title='已确认配图';description='接口测试';imageUrl='https://example.test/synthetic.png'}}
$m1 = Post-Json '/v2/note/module/add' $module
$m2 = Post-Json '/v2/note/module/add' $module
Assert-True ($m1.modules.Count -eq $m2.modules.Count) 'Module duplicated on retry'
Assert-True ($m2.content -eq $second.note.content) 'Module save overwrote body'
Write-Output "PASS voice-save/idempotency/payload-conflict/module-idempotency/body-preservation; synthetic note $($note.id)"

$fixture = Join-Path $PSScriptRoot '../ainote-product/repos/ainote_app/assets/images/todo_book.png'
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(180)
$client.DefaultRequestHeaders.Add('Authorization', "Guest $owner")
$client.DefaultRequestHeaders.Add('Device-Id', $owner)
try {
    $captured = @()
    1..2 | ForEach-Object {
        $form = [System.Net.Http.MultipartFormDataContent]::new()
        $file = [System.Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($fixture))
        $file.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/png')
        $form.Add($file, 'file', 'synthetic-fixture.png')
        $form.Add([System.Net.Http.StringContent]::new('接口测试文字：周六上午九点开会，讨论产品发布计划，需准备演示稿。'), 'recognizedText')
        $form.Add([System.Net.Http.StringContent]::new('final-capture-20260906'), 'requestId')
        $response = $client.PostAsync("$BaseUrl/v2/note/createImageNote", $form).GetAwaiter().GetResult()
        $data = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        Assert-True ($response.IsSuccessStatusCode -and $data.code -eq 200) "Capture create failed: $($data | ConvertTo-Json -Depth 3 -Compress)"
        $captured += $data.data
        $form.Dispose()
    }
    Assert-True ($captured[0].id -eq $captured[1].id) 'Capture retry duplicated note'
    $url = "$BaseUrl$($captured[0].imageUrl)"
    $ok = Invoke-WebRequest $url -Headers $headers
    Assert-True ($ok.StatusCode -eq 200 -and $ok.Headers.'Cache-Control' -match 'no-store') 'Private image owner/cache policy'
    $denied = Invoke-WebRequest $url -Headers @{Authorization='Guest synthetic-other';'Device-Id'='synthetic-other'} -SkipHttpErrorCheck
    $anonymous = Invoke-WebRequest $url -SkipHttpErrorCheck
    Assert-True ($denied.StatusCode -in @(401,403,404)) 'Wrong owner obtained image'
    Assert-True ($anonymous.StatusCode -in @(401,403,404)) 'Anonymous obtained image'
    Write-Output "PASS capture/create/retry/private-owner/no-store/wrong-owner-denied/anonymous-denied; synthetic note $($captured[0].id)"
} finally { $client.Dispose() }

$mergeBody = @{sourceNoteIds=@('urn:note:7502045542348222464','urn:note:7502045543627485184');selectionConfirmed=$true;idempotencyKey='final-bound-merge-20260906'}
$merge = Post-Json '/note/theme/urn:notetheme:7502045543673622528/merge' $mergeBody
$mergeRetry = Post-Json '/note/theme/urn:notetheme:7502045543673622528/merge' $mergeBody
Assert-True ($merge.note.id -eq $mergeRetry.note.id) 'Merge retry returned another result'
Write-Output "PASS merge-persistent-retry"
$products = @(Post-Json '/v2/note/analysis/productRecommendations' @{recordId='snapshot-79dff44c4a8c0aea37df26d3834b2bb68bdadf133f96d963b1401c6d6fd12dc6'})
foreach($product in $products) {
    Assert-True ($product.constraintVersion -eq 2) 'Outdated product safety decision'
    Assert-True ($product.recommendationReason -notmatch '排除|不符合|不适合|不能确认|无法确认') 'Contradictory excluded product returned'
}
Write-Output ($products | Select-Object productName,sourceType,recommendationReason,productShortUrl | ConvertTo-Json -Depth 5)
Write-Output "PRODUCTS $($products.Count) real results, review constraints above; this does not verify physical-device OCR."
