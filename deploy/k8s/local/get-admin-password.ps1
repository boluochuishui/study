param([string]$Namespace = "content-risk")

$ErrorActionPreference = "Stop"
if ((kubectl config current-context) -ne "docker-desktop") {
    throw "Kubernetes context must be docker-desktop"
}
# 仅在用户主动执行时展示本地集群的引导密码，部署日志不打印密码。
$encodedPassword = kubectl -n $Namespace get secret content-risk-local-secret `
    -o 'jsonpath={.data.ADMIN_BOOTSTRAP_PASSWORD}'
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($encodedPassword)) {
    throw "Admin bootstrap password is unavailable"
}
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encodedPassword))
