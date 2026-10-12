param(
    [string]$MysqlPassword = "abc123456",
    [string]$AuthAppId = "demo-app",
    [string]$AuthAppSecret = "demo-secret",
    [string]$ServiceDrainToken = "",
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
    } finally {
        Pop-Location
    }
}

Invoke-CheckedCommand "Namespace apply" {
    kubectl apply -f (Join-Path $PSScriptRoot "namespace.yaml")
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
$secretYaml = kubectl -n $Namespace create secret generic content-risk-local-secret `
    --from-literal="MYSQL_PASSWORD=$MysqlPassword" `
    --from-literal="AUTH_APP_ID=$AuthAppId" `
    --from-literal="AUTH_APP_SECRET=$AuthAppSecret" `
    --from-literal="SERVICE_DRAIN_TOKEN=$ServiceDrainToken" `
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
    "access-service"
)
foreach ($workload in $workloads) {
    Invoke-CheckedCommand "$workload rollout" {
        kubectl -n $Namespace rollout status "deployment/$workload" --timeout=180s
    }
}

Invoke-CheckedCommand "Resource listing" {
    kubectl -n $Namespace get pods,services,persistentvolumeclaims -o wide
}
