param(
    [Parameter(Mandatory = $true)]
    [string]$Milestone,
    [switch]$Push
)

$ErrorActionPreference = "Stop"

if ($Milestone -notmatch "^\d+\.\d+$") {
    throw "Milestone must use '<major>.<minor>' format, for example: 0.1"
}

$status = git status --porcelain
if (-not [string]::IsNullOrWhiteSpace($status)) {
    throw "Working tree is not clean. Commit or stash changes before tagging."
}

$tagName = "v$Milestone.0"

git rev-parse --verify "refs/tags/$tagName" *> $null
if ($LASTEXITCODE -eq 0) {
    throw "Tag '$tagName' already exists."
}

Write-Host "Creating annotated milestone tag: $tagName"
git tag -a $tagName -m "Roadmap milestone $Milestone complete"

if ($LASTEXITCODE -ne 0) {
    throw "Failed to create tag '$tagName'."
}

if ($Push) {
    Write-Host "Pushing tag $tagName to origin..."
    git push origin $tagName
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to push tag '$tagName'."
    }
}

Write-Host "Milestone tag created: $tagName"
