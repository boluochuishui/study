param(
    [string]$MysqlPassword = "abc123456",
    [string]$AuthAppId = "demo-app",
    [string]$AuthAppSecret = "demo-secret",
    [string]$ServiceDrainToken = "",
    [string]$AdminBootstrapPassword = $env:ADMIN_BOOTSTRAP_PASSWORD,
    [string]$ConsoleNodeImage = "",
    [string]$ConsoleNginxImage = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "../../..")
$Namespace = "content-risk"

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Description,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed"
    }
}

if ((kubectl config current-context) -ne "docker-desktop") {
    throw "Kubernetes context must be docker-desktop"
}

if (-not $SkipBuild) {
    Push-Location $projectRoot
    try {
        Invoke-CheckedCommand "Maven package" { mvn package -DskipTests }
        Invoke-CheckedCommand "Access service image build" {
            docker build -t content-risk/access-service:local -f Dockerfile .
        }
        Invoke-CheckedCommand "Text service image build" {
            docker build -t content-risk/text-detect-service:local -f text-detect-service/Dockerfile .
        }
        Invoke-CheckedCommand "Image service image build" {
            docker build -t content-risk/image-detect-service:local -f image-detect-service/Dockerfile .
        }
        Invoke-CheckedCommand "Audio service image build" {
            docker build -t content-risk/audio-detect-service:local -f audio-detect-service/Dockerfile .
        }
        Invoke-CheckedCommand "Video service image build" {
            docker build -t content-risk/video-detect-service:local -f video-detect-service/Dockerfile .
        }
        Invoke-CheckedCommand "Admin service image build" {
            docker build -t content-risk/admin-service:local -f admin-service/Dockerfile .
        }
        # 本地优先使用已有兼容镜像，避免无必要的 Docker Hub 下载。
        $cachedImages = @(docker images --format '{{.Repository}}:{{.Tag}}')
        if ($LASTEXITCODE -ne 0) {
            throw "Docker image inventory failed"
        }
        if ([string]::IsNullOrWhiteSpace($ConsoleNodeImage)) {
            $ConsoleNodeImage = "node:24-alpine"
            foreach ($candidate in @("node:24-alpine", "node:24", "node:22")) {
                if ($cachedImages -contains $candidate) {
                    $ConsoleNodeImage = $candidate
                    break
                }
            }
        }
        if ([string]::IsNullOrWhiteSpace($ConsoleNginxImage)) {
            $ConsoleNginxImage = "nginx:1.29-alpine"
            foreach ($candidate in @("nginx:1.29-alpine", "nginx:alpine")) {
                if ($cachedImages -contains $candidate) {
                    $ConsoleNginxImage = $candidate
                    break
                }
            }
        }
        Write-Host "Console base images: $ConsoleNodeImage, $ConsoleNginxImage"
        Invoke-CheckedCommand "Console image build" {
            docker build -t content-risk/study-console-web:local `
                --build-arg "NODE_IMAGE=$ConsoleNodeImage" `
                --build-arg "NGINX_IMAGE=$ConsoleNginxImage" `
                --build-arg VITE_APP_ENV=local-k8s -f study-console-web/Dockerfile study-console-web
        }
    } finally {
        Pop-Location
    }
}

Invoke-CheckedCommand "Namespace apply" {
    kubectl apply -f (Join-Path $PSScriptRoot "namespace.yaml")
}
$existingSecret = kubectl -n $Namespace get secret content-risk-local-secret --ignore-not-found -o json
if ($LASTEXITCODE -ne 0) {
    throw "Local secret lookup failed"
}
$secretData = $null
if ($existingSecret) {
    $secretData = ($existingSecret | ConvertFrom-Json).data
}
if ([string]::IsNullOrWhiteSpace($ServiceDrainToken) -and $null -ne $secretData `
        -and $secretData.PSObject.Properties.Name -contains "SERVICE_DRAIN_TOKEN") {
    $ServiceDrainToken = [Text.Encoding]::UTF8.GetString(
        [Convert]::FromBase64String($secretData.SERVICE_DRAIN_TOKEN))
}
if ([string]::IsNullOrWhiteSpace($ServiceDrainToken)) {
    $tokenBytes = New-Object byte[] 32
    $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomGenerator.GetBytes($tokenBytes)
        $ServiceDrainToken = [BitConverter]::ToString($tokenBytes).Replace("-", "")
    } finally {
        $randomGenerator.Dispose()
    }
}
# 复用首次创建的引导密码；已有用户时不会因部署重置密码。
if ([string]::IsNullOrWhiteSpace($AdminBootstrapPassword)) {
    if ($null -ne $secretData) {
        if ($secretData.PSObject.Properties.Name -contains "ADMIN_BOOTSTRAP_PASSWORD") {
            $AdminBootstrapPassword = [Text.Encoding]::UTF8.GetString(
                [Convert]::FromBase64String($secretData.ADMIN_BOOTSTRAP_PASSWORD))
        }
    }
    if ([string]::IsNullOrWhiteSpace($AdminBootstrapPassword)) {
        $passwordBytes = New-Object byte[] 24
        $passwordGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $passwordGenerator.GetBytes($passwordBytes)
            $AdminBootstrapPassword = [Convert]::ToBase64String($passwordBytes)
        } finally {
            $passwordGenerator.Dispose()
        }
    }
}
if ($AdminBootstrapPassword.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($AdminBootstrapPassword) -gt 72) {
    throw "Admin bootstrap password must contain at least 12 characters and at most 72 UTF-8 bytes"
}
$secretYaml = kubectl -n $Namespace create secret generic content-risk-local-secret `
    --from-literal="MYSQL_PASSWORD=$MysqlPassword" `
    --from-literal="AUTH_APP_ID=$AuthAppId" `
    --from-literal="AUTH_APP_SECRET=$AuthAppSecret" `
    --from-literal="SERVICE_DRAIN_TOKEN=$ServiceDrainToken" `
    --from-literal="ADMIN_BOOTSTRAP_PASSWORD=$AdminBootstrapPassword" `
    --dry-run=client -o yaml
if ($LASTEXITCODE -ne 0) {
    throw "Local secret generation failed"
}
$secretYaml | kubectl apply -f -
if ($LASTEXITCODE -ne 0) {
    throw "Local secret apply failed"
}

Invoke-CheckedCommand "Kubernetes resources apply" { kubectl apply -k $PSScriptRoot }
Invoke-CheckedCommand "MySQL rollout" {
    kubectl -n $Namespace rollout status deployment/mysql --timeout=180s
}
Invoke-CheckedCommand "Redis rollout" {
    kubectl -n $Namespace rollout status deployment/redis --timeout=120s
}

$workloads = @(
    "text-detect-service",
    "image-detect-service",
    "audio-detect-service",
    "video-detect-service",
    "access-service",
    "admin-service",
    "study-console-web"
)
# 本地标签固定为 local，重建镜像后需要重启工作负载才能运行新版本。
if (-not $SkipBuild) {
    foreach ($workload in $workloads) {
        Invoke-CheckedCommand "$workload restart" {
            kubectl -n $Namespace rollout restart "deployment/$workload"
        }
    }
}
foreach ($workload in $workloads) {
    Invoke-CheckedCommand "$workload rollout" {
        kubectl -n $Namespace rollout status "deployment/$workload" --timeout=180s
    }
}

Write-Host "Console: http://localhost:30088 (tenant: default, username: admin)"
Write-Host "Read the bootstrap password with deploy/k8s/local/get-admin-password.ps1."

Invoke-CheckedCommand "Resource listing" {
    kubectl -n $Namespace get pods,services,persistentvolumeclaims -o wide
}
