# Verification Script for TestReason (JSTQB-Pocket-Trainer)
$ErrorActionPreference = "Stop"

Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

Write-Host "=========================================="
Write-Host "1. Running Unit Tests"
Write-Host "=========================================="
cmd /c "gradlew.bat testDebugUnitTest --console=plain"
$testExitCode = $LASTEXITCODE

if ($testExitCode -ne 0) {
    Write-Error "Unit tests failed with exit code $testExitCode"
    exit $testExitCode
}

# Parse test XML reports to count actual executed unit tests
$testFiles = Get-ChildItem -Path "." -Recurse -Filter "TEST-*.xml"
$totalTests = 0
$totalFailures = 0
$totalSkipped = 0

foreach ($file in $testFiles) {
    [xml]$xml = Get-Content $file.FullName
    if ($xml.testsuite) {
        $totalTests += [int]$xml.testsuite.tests
        $totalFailures += [int]$xml.testsuite.failures + [int]$xml.testsuite.errors
        $totalSkipped += [int]$xml.testsuite.skipped
    }
}

Write-Host "------------------------------------------"
Write-Host "Test Summary: Total=$totalTests, Failures=$totalFailures, Skipped=$totalSkipped"
Write-Host "------------------------------------------"

if ($totalTests -eq 0) {
    Write-Error "No unit tests were found or executed!"
    exit 1
}

Write-Host "=========================================="
Write-Host "2. Building Debug APK"
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
Write-Host "VERIFICATION PASSED SUCCESSFULLY"
Write-Host "=========================================="
exit 0
