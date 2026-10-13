param(
    [string]$Endpoint = "http://localhost:30080/api/detect/sync",
    [string]$AppId = "demo-app",
    [string]$AppSecret = "demo-secret",
    [string]$Text = "normal content",
    [string]$SceneCode = "comment"
)

$ErrorActionPreference = "Stop"

function ConvertTo-LowerHex {
    param([byte[]]$Bytes)

    return ([BitConverter]::ToString($Bytes) -replace "-", "").ToLowerInvariant()
}

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString()
$nonce = [Guid]::NewGuid().ToString()
$requestBody = [ordered]@{
    clientRequestId = "local-k8s-$([Guid]::NewGuid().ToString('N'))"
    sceneCode = $SceneCode
    contentType = "TEXT"
    items = @(@{ itemId = "one"; text = $Text })
}
$body = $requestBody | ConvertTo-Json -Depth 5 -Compress
$bodyBytes = [Text.Encoding]::UTF8.GetBytes($body)
$sha256 = [Security.Cryptography.SHA256]::Create()
$bodyDigest = ConvertTo-LowerHex ($sha256.ComputeHash($bodyBytes))
$path = ([Uri]$Endpoint).AbsolutePath
$canonicalPayload = "POST`n$path`n$AppId`n$timestamp`n$nonce`n$bodyDigest"
$hmac = [Security.Cryptography.HMACSHA256]::new()
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($AppSecret)
$signature = ConvertTo-LowerHex ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($canonicalPayload)))

try {
    Invoke-RestMethod -Method Post -Uri $Endpoint -ContentType "application/json; charset=utf-8" `
        -Headers @{
            "X-App-Id" = $AppId
            "X-Timestamp" = $timestamp
            "X-Nonce" = $nonce
            "X-Signature" = $signature
        } `
        -Body $bodyBytes
} finally {
    $sha256.Dispose()
    $hmac.Dispose()
}
