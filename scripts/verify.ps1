# Verification Script for TestReason (JSTQB-Pocket-Trainer)
$ErrorActionPreference = "Stop"

Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

# JDK 17 Resolution
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $searchPaths = @(
        "C:\Program Files\Java\jdk-17*",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "$env:USERPROFILE\.jdks\jdk-17*",
        "C:\Program Files\Java\jdk*"
    )
    foreach ($pathPattern in $searchPaths) {
        $found = Get-Item $pathPattern -ErrorAction SilentlyContinue | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
        if ($found) {
            $env:JAVA_HOME = $found.FullName
            break
        }
    }
}

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

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "JAVA_HOME could not be resolved! Please set JAVA_HOME environment variable to JDK 17."
    exit 1
}

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    Write-Error "ANDROID_HOME could not be resolved! Please set ANDROID_HOME environment variable."
    exit 1
}

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
Write-Host "Using ANDROID_HOME=$env:ANDROID_HOME"

# Clean old test results prior to running tests
Get-ChildItem -Path "." -Recurse -Filter "TEST-*.xml" | Remove-Item -Force -ErrorAction SilentlyContinue

Write-Host "=========================================="
Write-Host "1. Running Unit Tests"
Write-Host "=========================================="
cmd /c "gradlew.bat testDebugUnitTest --console=plain"
$testExitCode = $LASTEXITCODE

if ($testExitCode -ne 0) {
    Write-Error "Unit tests failed with exit code $testExitCode"
    exit $testExitCode
}

# Parse test XML reports from CURRENT test run only
$testFiles = Get-ChildItem -Path "." -Recurse -Filter "TEST-*.xml"
$totalTestSuites = $testFiles.Count
$totalTestCases = 0
$totalFailures = 0
$totalSkipped = 0

foreach ($file in $testFiles) {
    [xml]$xml = Get-Content $file.FullName
    if ($xml.testsuite) {
        $totalTestCases += [int]$xml.testsuite.tests
        $totalFailures += [int]$xml.testsuite.failures + [int]$xml.testsuite.errors
        $totalSkipped += [int]$xml.testsuite.skipped
    }
}

Write-Host "------------------------------------------"
Write-Host "Test Summary: SuitedCount=$totalTestSuites, TestCaseCount=$totalTestCases, Failures=$totalFailures, Skipped=$totalSkipped"
Write-Host "------------------------------------------"

if ($totalTestCases -eq 0) {
    Write-Error "No unit testcases were found or executed!"
    exit 1
}

if ($totalSkipped -ge $totalTestCases) {
    Write-Error "All unit testcases were skipped!"
    exit 1
}

if ($totalFailures -gt 0) {
    Write-Error "$totalFailures unit testcase(s) failed!"
    exit 1
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
Write-Host "3. Running Detekt Static Analysis"
Write-Host "=========================================="
cmd /c "gradlew.bat detekt --console=plain"
$detektExitCode = $LASTEXITCODE

if ($detektExitCode -ne 0) {
    Write-Host "Detekt generated advisory warnings (non-blocking)"
}

Write-Host "=========================================="
Write-Host "4. Building Debug APK"
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
