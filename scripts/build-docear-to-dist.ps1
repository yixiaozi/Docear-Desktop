# Build Docear from this repository and publish artifacts to E:\Develop\dist
# Usage (from repo root or anywhere):
#   powershell -ExecutionPolicy Bypass -File .\scripts\build-docear-to-dist.ps1
# Optional:
#   -SkipBuild     only copy existing files from docear_framework\dist (no ant)

param(
    [switch] $SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$antPath = Join-Path $repoRoot "tools\apache-ant-1.10.14\bin\ant.bat"
$buildFile = Join-Path $repoRoot "docear_framework\ant\build.xml"
$distDir = Join-Path $repoRoot "docear_framework\dist"
$targetDir = "E:\Develop\dist"

$candidates = @(
    "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot",
    "C:\Program Files\Eclipse Adoptium\jdk-8.0.412.8-hotspot",
    "C:\Program Files\Eclipse Adoptium\jdk-8.0.392.8-hotspot"
)
foreach ($jdk in $candidates) {
    if (Test-Path $jdk) {
        $env:JAVA_HOME = $jdk
        $env:Path = "$($env:JAVA_HOME)\bin;$($env:Path)"
        break
    }
}

if (!(Test-Path $antPath)) {
    throw "Ant not found at $antPath"
}
if (!(Test-Path $buildFile)) {
    throw "Build file not found at $buildFile"
}

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & $antPath -f $buildFile docear-dist
        if ($LASTEXITCODE -ne 0) {
            throw "Docear build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

$patterns = @("docear_*.zip", "docear_*.tar.gz", "docear_*.zip.MD5", "docear_*.tar.gz.MD5", "gitinfo-*.txt", "history_en.txt")
foreach ($pat in $patterns) {
    Get-ChildItem -Path $distDir -Filter $pat -ErrorAction SilentlyContinue | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $targetDir $_.Name) -Force
    }
}

$windowsZip = Join-Path $distDir "docear_windows.zip"
if (!(Test-Path $windowsZip)) {
    throw "Expected package not found: $windowsZip"
}

Write-Output "Published Docear packages to $targetDir"
