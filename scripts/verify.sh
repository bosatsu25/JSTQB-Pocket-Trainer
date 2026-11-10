#!/usr/bin/env bash
# Verification script for TestReason (Linux/macOS). Mirrors scripts/verify.ps1 and CI.
set -euo pipefail
cd "$(dirname "$0")/.."

java_home="${JAVA_HOME:-}"
if [[ -z "$java_home" || ! -x "$java_home/bin/java" ]]; then
  echo "JAVA_HOME must point to JDK 17." >&2
  exit 1
fi
if ! "$java_home/bin/java" -version 2>&1 | grep -Eq 'version "17(\.|")'; then
  echo "JAVA_HOME ($java_home) is not JDK 17." >&2
  exit 1
fi
if [[ -z "${ANDROID_HOME:-}" ]] && ! grep -q '^sdk.dir=' local.properties 2>/dev/null; then
  echo "Set ANDROID_HOME or sdk.dir in local.properties." >&2
  exit 1
fi

./gradlew assembleDebug testDebugUnitTest lintDebug detekt --console=plain
echo "ALL VERIFICATION GATES PASSED"
