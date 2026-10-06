---
name: verify-change
description: Performs mechanical verification including build, unit tests, static analysis, and git diff review.
---

# Verify Change Skill

Use this skill to verify code or documentation changes against Quality Gates.

## Verification Checklist

1. **Build Check**:
   - Verify `gradlew assembleDebug` or target build succeeds without errors.

2. **Test Execution**:
   - Run relevant unit tests (`gradlew testDebugUnitTest`).
   - Confirm 0 test failures.

3. **Static Analysis**:
   - Run Android Lint / Detekt checks.
   - Confirm no new high-severity warnings or errors.

4. **Self Diff Review**:
   - Review `git diff` against Acceptance Criteria and `AGENTS.md` invariants.
   - Ensure no leftover debug statements or unhandled TODOs.
