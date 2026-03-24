$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$appHome = Resolve-Path (Join-Path $scriptDir "..")
$pidFile = Join-Path $appHome "run\eam.pid"

if (-not (Test-Path $pidFile)) {
  Write-Host "未找到 PID 文件，无需停止"
  exit 0
}

$pid = Get-Content $pidFile -ErrorAction SilentlyContinue
if (-not $pid) {
  Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
  Write-Host "PID 文件为空，已清理"
  exit 0
}

$process = Get-Process -Id $pid -ErrorAction SilentlyContinue
if ($process) {
  Stop-Process -Id $pid -Force
  Write-Host "已停止进程，PID=$pid"
} else {
  Write-Host "PID=$pid 对应进程不存在"
}

Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
