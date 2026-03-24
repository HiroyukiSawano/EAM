$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$appHome = Resolve-Path (Join-Path $scriptDir "..")
$pidFile = Join-Path $appHome "run\eam.pid"
$logDir = Join-Path $appHome "logs"
$runDir = Join-Path $appHome "run"
$jarFile = Join-Path $appHome "eam-app.jar"
$configDir = Join-Path $appHome "config"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

if (Test-Path $pidFile) {
  $existingPid = Get-Content $pidFile -ErrorAction SilentlyContinue
  if ($existingPid) {
    $process = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
    if ($process) {
      Write-Host "EAM 后端已在运行，PID=$existingPid"
      exit 0
    }
  }
}

$stdoutFile = Join-Path $logDir "app.out.log"
$stderrFile = Join-Path $logDir "app.err.log"
$arguments = @(
  "-jar"
  "`"$jarFile`""
  "--spring.profiles.active=prod"
  "--spring.config.additional-location=file:$configDir\"
)

$process = Start-Process -FilePath "java" -ArgumentList $arguments -WorkingDirectory $appHome -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile -PassThru
$process.Id | Set-Content -Path $pidFile -Encoding ASCII

Write-Host "EAM 后端已启动，PID=$($process.Id)"
