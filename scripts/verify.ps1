# Verification Script for TestReason (JSTQB-Pocket-Trainer)
$ErrorActionPreference = "Stop"

Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

# Dynamic JAVA_HOME resolution
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $searchPaths = @(
        "C:\Program Files\Java\jdk*",
        "C:\Program Files\Eclipse Adoptium\jdk*",
        "$env:USERPROFILE\.jdks\*",
        "C:\Program Files\Android\Android Studio\jbr"
    )
    foreach ($pathPattern in $searchPaths) {
        $found = Get-Item $pathPattern -ErrorAction SilentlyContinue | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
        if ($found) {
            $env:JAVA_HOME = $found.FullName
            break
        }
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaBinDir = Split-Path -Path $javaCmd.Source -Parent
        $env:JAVA_HOME = Split-Path -Path $javaBinDir -Parent
    }
}

# Dynamic Android SDK resolution
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
    Write-Error "JAVA_HOME could not be resolved! Please set JAVA_HOME environment variable."
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

if ($totalSkipped -ge $totalTests) {
    Write-Error "All unit tests were skipped!"
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
Write-Host "3. Running Detekt Static Analysis Across Modules"
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
