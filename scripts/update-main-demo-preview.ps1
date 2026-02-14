param(
    [string]$OutputPath = "docs/images/main-demo.png"
)

$ErrorActionPreference = "Stop"

Write-Host "Updating MainDemo preview image..."
Write-Host "Output: $OutputPath"

& "$PSScriptRoot\..\gradlew.bat" captureMainDemoScreenshot "-PsnapfxScreenshotOutput=$OutputPath"

if ($LASTEXITCODE -ne 0) {
    throw "Preview generation failed with exit code $LASTEXITCODE."
}

Write-Host "MainDemo preview updated successfully."
