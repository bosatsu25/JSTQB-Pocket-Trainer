# Verification Script for TestReason (JSTQB-Pocket-Trainer)
$ErrorActionPreference = "Stop"

Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

# Strict JDK 17 Validation
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "JAVA_HOME environment variable is not set or invalid! Please set JAVA_HOME to JDK 17."
    exit 1
}

$javaVersionOutput = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Out-String
Write-Host "Java Version Output: $javaVersionOutput"

if ($javaVersionOutput -notmatch 'version "(17\.[0-9_]+|17)"') {
    Write-Error "JAVA_HOME must point to JDK 17! Current JAVA_HOME ($env:JAVA_HOME) is not JDK 17."
    exit 1
}

# Android SDK Resolution
if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    if (Test-Path "local.properties") {
        $localProps = Get-Content "local.properties" | Select-String "sdk.dir"
        if ($localProps) {
            $env:ANDROID_HOME = ($localProps -split "=")[1].Trim().Replace("\\", "\")
        }
    }
    if (-not $env:ANDROID_HOME -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
}

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    Write-Error "ANDROID_HOME could not be resolved! Please set ANDROID_HOME environment variable."
    exit 1
}

Write-Host "Using JDK 17 at JAVA_HOME=$env:JAVA_HOME"
Write-Host "Using ANDROID_HOME=$env:ANDROID_HOME"

Write-Host "=========================================="
Write-Host "1. Running Unit Tests"
Write-Host "=========================================="
cmd /c "gradlew.bat testDebugUnitTest --console=plain"
$testExitCode = $LASTEXITCODE

if ($testExitCode -ne 0) {
    Write-Error "Unit tests failed with exit code $testExitCode"
    exit $testExitCode
}

Write-Host "=========================================="
Write-Host "2. Running Android Lint Check"
Write-Host "=========================================="
cmd /c "gradlew.bat lintDebug --console=plain"
$lintExitCode = $LASTEXITCODE

if ($lintExitCode -ne 0) {
    Write-Error "Android Lint check failed with exit code $lintExitCode"
    exit $lintExitCode
}

Write-Host "=========================================="
Write-Host "3. Building Debug APK"
Write-Host "=========================================="
cmd /c "gradlew.bat assembleDebug --console=plain"
$buildExitCode = $LASTEXITCODE

if ($buildExitCode -ne 0) {
    Write-Error "Assembly failed with exit code $buildExitCode"
    exit $buildExitCode
}

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length
    Write-Host "Debug APK successfully generated at $apkPath ($apkSize bytes)"
} else {
    Write-Error "APK file was not found at $apkPath"
    exit 1
}

Write-Host "=========================================="
Write-Host "ALL VERIFICATION GATES PASSED SUCCESSFULLY"
Write-Host "=========================================="
exit 0
