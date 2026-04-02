param(
  [switch]$SkipBuild,
  [switch]$NoPause,
  [string]$MavenRepoLocal,
  [string]$MavenSettings
)

$ErrorActionPreference = "Stop"

$commandLineArgs = [Environment]::GetCommandLineArgs()
$launchedViaFile = $commandLineArgs -contains "-File"

try {
  $appHome = Resolve-Path (Join-Path $PSScriptRoot "..")
  $frontendHome = Resolve-Path (Join-Path $appHome "..\dg_admin_web_next")
  $releaseDir = Join-Path $appHome "release\eam-backend"
  $defaultUserRepo = Join-Path $HOME ".m2\repository"
  $effectiveMavenRepo = if ($MavenRepoLocal) { $MavenRepoLocal } else { $defaultUserRepo }

  New-Item -ItemType Directory -Force -Path $effectiveMavenRepo | Out-Null

  if (-not $SkipBuild) {
    Push-Location $frontendHome
    try {
      & npm.cmd run build
      if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed. Integrated release packaging stopped."
      }
    } finally {
      Pop-Location
    }

    Push-Location $appHome
    try {
      $mavenArgs = @("clean", "package", "-DskipTests", "-Dmaven.repo.local=$effectiveMavenRepo")
      if ($MavenSettings) {
        $mavenArgs = @("-gs", $MavenSettings) + $mavenArgs
      }
      & mvn @mavenArgs
      if ($LASTEXITCODE -ne 0) {
        throw "Maven packaging failed. Integrated release packaging stopped. Maven local repo: $effectiveMavenRepo"
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
    throw "No publishable JAR file was found."
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

  Write-Host "Backend release package created: $releaseDir"
  Write-Host "Maven local repository: $effectiveMavenRepo"
  if ($MavenSettings) {
    Write-Host "Maven settings file: $MavenSettings"
  } else {
    Write-Host "Maven settings file: using default system settings.xml"
  }
} catch {
  Write-Error $_
  exit 1
} finally {
  if ($launchedViaFile -and -not $NoPause) {
    Read-Host "Press Enter to close this window"
  }
}
