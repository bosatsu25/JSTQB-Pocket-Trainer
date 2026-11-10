# Quality Gates Specification: TestReason

---

## 1. Automated Quality Gate Criteria

Every Wave / PR must satisfy the following Quality Gates:

1. **Compilation & Build**:
   - `gradlew assembleDebug` must compile cleanly without errors.

2. **Unit & Integration Tests**:
   - `gradlew testDebugUnitTest` must pass with 0 failures.
   - Every new or changed ViewModel needs a ViewModel test (real repository on in-memory Room via the
     `TestEnvironment` helper in each feature module's tests, `MainDispatcherRule`, Turbine for one-off events).
   - Every new screen needs a stateless `*Content` composable covered by the responsive suite
     (docs/quality/TEST_STRATEGY.md §2).

3. **Static Analysis & Code Style**:
   - `gradlew lintDebug` must produce no new errors or high-severity warnings.
   - `gradlew detekt` (when configured) must pass code quality inspection.

4. **Architecture & Dependency Check**:
   - No cyclic module dependencies.
   - Core layers must not depend on feature modules.

5. **Diff Review & Evidence**:
   - `git diff` must be reviewed to ensure no temporary debug code, commented-out code, or unhandled TODOs are left in PRs.
