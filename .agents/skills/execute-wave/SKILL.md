---
name: execute-wave
description: Executes a single wave from the roadmap following the mandatory execution loop and quality gates, then advances continuously to the next wave upon passing verification.
---

# Wave Execution Skill

Use this skill when executing Waves from `docs/roadmap/ROADMAP.md`.

## Execution Workflow

1. **Observe State**:
   - Run `git status` to verify workspace clean state.
   - Read `docs/roadmap/CURRENT_STATE.md` and `docs/roadmap/ROADMAP.md`.

2. **Select Wave**:
   - Identify the single next uncompleted Wave whose dependencies are satisfied.
   - Declare Goal, Scope, Acceptance Criteria, Risks, and Verification plan before making changes.

3. **Minimal Implementation**:
   - Implement only the minimal, cohesive changes required for the selected Wave.

4. **Verify Change**:
   - Execute build, unit tests, static analysis, and relevant UI tests.
   - Do NOT mark DONE unless all verification gates pass.

5. **Update State & Advance**:
   - Update `docs/roadmap/CURRENT_STATE.md` with execution summary and verification evidence.
   - If verification passes, automatically proceed to the next eligible Wave in the roadmap until milestone completion or encountering a stop condition.
