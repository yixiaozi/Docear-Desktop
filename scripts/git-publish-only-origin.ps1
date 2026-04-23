# Use only your own GitHub repo for push; optional read-only upstream for syncing.
# Run from repo root: powershell -ExecutionPolicy Bypass -File .\scripts\git-publish-only-origin.ps1
#
# Environment (optional):
#   $env:GIT_MY_REPO_URL = "https://github.com/<you>/Docear-Desktop.git"
#   $env:GIT_UPSTREAM_FETCH_URL = "https://github.com/BeelGroup/Docear-Desktop.git"  # omit to skip upstream

param(
    [string] $MyRepoUrl = $env:GIT_MY_REPO_URL,
    [string] $UpstreamFetchOnlyUrl = $env:GIT_UPSTREAM_FETCH_URL
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

if (-not $MyRepoUrl) {
    $MyRepoUrl = (git remote get-url origin 2>$null)
}
if (-not $MyRepoUrl) {
    throw "Set GIT_MY_REPO_URL or ensure origin exists."
}

git remote remove upstream 2>$null

git remote set-url origin $MyRepoUrl
git config --local remote.pushDefault origin
git config --local push.default simple

if ($UpstreamFetchOnlyUrl) {
    git remote add upstream $UpstreamFetchOnlyUrl
    git remote set-url --push upstream "https://invalid.invalid/no-push-to-upstream"
}

Write-Output "origin (fetch/push): $(git remote get-url origin)"
if (git remote get-url upstream 2>$null) {
    Write-Output "upstream (fetch only): $(git remote get-url upstream)"
}
Write-Output "Default push remote: $(git config --get remote.pushDefault)"
