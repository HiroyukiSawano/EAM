param(
  [string]$ServiceName = "eam-backend",
  [string]$NssmPath = "C:\nssm\nssm.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $NssmPath)) {
  throw "未找到 nssm，请先安装并修改脚本中的 NssmPath。"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$appHome = Resolve-Path (Join-Path $scriptDir "..")
$jarFile = Join-Path $appHome "eam-app.jar"
$configDir = Join-Path $appHome "config"
$logDir = Join-Path $appHome "logs"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

& $NssmPath install $ServiceName "java" "-jar `"$jarFile`" --spring.profiles.active=prod --spring.config.additional-location=file:$configDir\"
& $NssmPath set $ServiceName AppDirectory $appHome
& $NssmPath set $ServiceName AppStdout (Join-Path $logDir "service.out.log")
& $NssmPath set $ServiceName AppStderr (Join-Path $logDir "service.err.log")
& $NssmPath set $ServiceName Start SERVICE_AUTO_START

Write-Host "Windows 服务安装完成：$ServiceName"
