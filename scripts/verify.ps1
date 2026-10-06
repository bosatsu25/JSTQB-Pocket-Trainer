# Verification Script for TestReason (JSTQB-Pocket-Trainer)
$ErrorActionPreference = "Stop"

Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

# Auto-detect JAVA_HOME if not set or invalid
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $possibleJdks = @(
        "C:\Program Files\Java\jdk-23",
        "C:\Program Files\Android\Android Studio\jbr"
    )
    foreach ($jdk in $possibleJdks) {
        if (Test-Path "$jdk\bin\java.exe") {
            $env:JAVA_HOME = $jdk
            break
        }
    }
}

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
}

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
Write-Host "Using ANDROID_HOME=$env:ANDROID_HOME"

# Clean old XML test results prior to execution
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

# Parse test XML reports to count actual executed unit tests from CURRENT run
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

if ($totalFailures -gt 0) {
    Write-Error "$totalFailures unit test(s) failed!"
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
    Write-Error "Detekt static analysis failed with exit code $detektExitCode"
    exit $detektExitCode
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
