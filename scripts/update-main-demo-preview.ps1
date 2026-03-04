param(
    [string]$OutputPath = "docs/images/main-demo.png",
    [switch]$IncludeGif,
    [string]$GifOutputPath = "docs/images/main-demo.gif",
    [switch]$OptimizeGif,
    [int]$GifColors = 128,
    [int]$GifLossy = 80
)

$ErrorActionPreference = "Stop"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$isWindowsHost = $env:OS -eq "Windows_NT"
$gradleWrapper = if ($isWindowsHost) {
    Join-Path $projectRoot "gradlew.bat"
} else {
    Join-Path $projectRoot "gradlew"
}

Write-Host "Updating MainDemo preview image..."
Write-Host "Output: $OutputPath"

& $gradleWrapper :snapfx-demo:captureMainDemoScreenshot "-PsnapfxScreenshotOutput=$OutputPath"

if ($LASTEXITCODE -ne 0) {
    throw "Preview generation failed with exit code $LASTEXITCODE."
}

if ($IncludeGif) {
    Write-Host "Updating MainDemo preview animation..."
    Write-Host "GIF Output: $GifOutputPath"

    & $gradleWrapper :snapfx-demo:captureMainDemoGif "-PsnapfxGifOutput=$GifOutputPath"

    if ($LASTEXITCODE -ne 0) {
        throw "GIF preview generation failed with exit code $LASTEXITCODE."
    }

    if ($OptimizeGif) {
        $gifsicleCmd = Get-Command gifsicle -ErrorAction SilentlyContinue
        if ($null -eq $gifsicleCmd) {
            throw "GIF optimization requested, but 'gifsicle' was not found in PATH."
        }

        if ($GifColors -lt 2 -or $GifColors -gt 256) {
            throw "GifColors must be between 2 and 256."
        }
        if ($GifLossy -lt 0 -or $GifLossy -gt 200) {
            throw "GifLossy must be between 0 and 200."
        }

        $gifFile = Get-Item $GifOutputPath
        $originalBytes = $gifFile.Length
        $tempOptimizedPath = [System.IO.Path]::ChangeExtension([System.IO.Path]::GetTempFileName(), ".gif")

        Write-Host "Optimizing GIF via gifsicle (colors=$GifColors, lossy=$GifLossy)..."
        & $gifsicleCmd.Source "--optimize=3" "--colors=$GifColors" "--lossy=$GifLossy" "--output" "$tempOptimizedPath" "$GifOutputPath"
        if ($LASTEXITCODE -ne 0) {
            if (Test-Path $tempOptimizedPath) {
                Remove-Item $tempOptimizedPath -Force -ErrorAction SilentlyContinue
            }
            throw "GIF optimization failed with exit code $LASTEXITCODE."
        }

        Move-Item -Force $tempOptimizedPath $GifOutputPath
        $optimizedBytes = (Get-Item $GifOutputPath).Length
        if ($originalBytes -gt 0) {
            $savedPercent = [Math]::Round((($originalBytes - $optimizedBytes) * 100.0) / $originalBytes, 1)
            Write-Host "GIF optimized: $originalBytes -> $optimizedBytes bytes ($savedPercent% smaller)."
        }
    }
}

Write-Host "MainDemo preview updated successfully."
