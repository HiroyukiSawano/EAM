param(
  [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$appHome = Resolve-Path (Join-Path $PSScriptRoot "..")
$releaseDir = Join-Path $appHome "release\eam-backend"

if (-not $SkipBuild) {
  Push-Location $appHome
  try {
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
      throw "Maven 打包失败，已中止生成发布包。"
    }
  } finally {
    Pop-Location
  }
}

if (Test-Path $releaseDir) {
  Remove-Item -Recurse -Force $releaseDir
}

New-Item -ItemType Directory -Force -Path (Join-Path $releaseDir "config") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $releaseDir "scripts") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $releaseDir "sql\migration") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $releaseDir "systemd") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $releaseDir "windows") | Out-Null

$jarFile = Get-ChildItem -Path (Join-Path $appHome "target") -Filter *.jar | Where-Object { $_.Name -notlike "*.original" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jarFile) {
  throw "未找到可发布的 JAR 文件"
}

Copy-Item $jarFile.FullName (Join-Path $releaseDir "eam-app.jar")
Copy-Item (Join-Path $appHome "deploy\config\application-prod.yml") (Join-Path $releaseDir "config\application-prod.yml")
Copy-Item (Join-Path $appHome "deploy\sql\README.md") (Join-Path $releaseDir "sql\README.md")
Copy-Item (Join-Path $appHome "src\main\resources\db\migration\*.sql") (Join-Path $releaseDir "sql\migration")
Copy-Item (Join-Path $appHome "deploy\scripts\start.sh") (Join-Path $releaseDir "scripts\start.sh")
Copy-Item (Join-Path $appHome "deploy\scripts\stop.sh") (Join-Path $releaseDir "scripts\stop.sh")
Copy-Item (Join-Path $appHome "deploy\scripts\start.ps1") (Join-Path $releaseDir "scripts\start.ps1")
Copy-Item (Join-Path $appHome "deploy\scripts\stop.ps1") (Join-Path $releaseDir "scripts\stop.ps1")
Copy-Item (Join-Path $appHome "deploy\systemd\eam.service") (Join-Path $releaseDir "systemd\eam.service")
Copy-Item (Join-Path $appHome "deploy\windows\install-service.ps1") (Join-Path $releaseDir "windows\install-service.ps1")

Write-Host "后端发布包已生成：$releaseDir"
