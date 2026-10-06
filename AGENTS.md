# TestReason Agent Constitution (AGENTS.md)

This repository (`JSTQB-Pocket-Trainer`, product name: **TestReason**) uses a **Repository-Driven Agent Harness** to guide all AI agent and developer interactions.

---

## 1. Core Principles

1. **Repository is the Single Source of Truth**:
   - The agent's memory or chat history is NOT the source of truth.
   - All state, roadmap progress, specifications, and architecture decisions reside in the repository (`docs/`, `AGENTS.md`, `.agents/skills/`).

2. **Verified Progress vs. Code Generation**:
   - `Generated Code != Progress`.
   - `Verified Progress = Acceptance Criteria Satisfied AND Build Green AND Tests Green AND Static Analysis Green AND Diff Reviewed AND Documentation Updated`.

3. **Development Unit & Continuous Execution**:
   - All work is organized as: `Phase -> Wave -> PR` (bundled into Milestones M0–M4).
   - **1 Wave = 1 Cohesive PR/Commit**. Do NOT mix multiple unrelated concerns in a single Wave.
   - **Continuous Execution**: For each Wave, perform verification, review, and state updates. Once all Quality Gates pass, automatically proceed to the next eligible Wave that meets its dependencies, continuing until the target milestone is reached or a stop condition is triggered.

4. **YAGNI & Pragmatic Clean Architecture**:
   - Do NOT over-engineer. Avoid producing unnecessary Repositories, UseCases, Mappers, or Providers unless required by complex business logic.
   - UI layer uses Unidirectional Data Flow (UDF) with Compose and ViewModels: **State Down, Events Up**.
   - **Data Boundaries**:
     - **Room**: Persistent application data (Questions, Question versions, Sessions, Order, Attempts, Finalized states, Review schedules).
     - **DataStore**: User preferences (Theme, Daily study goals, Notification settings).
     - **SavedStateHandle / UI State**: Lightweight navigation and input restoration state.

5. **No Hallucinated API / Library Versions**:
   - Do NOT rely on model memory alone for dependencies or API signatures.
   - Consult official Android Knowledge Base / Android Docs prior to introducing or updating library APIs.

---

## 2. Mandatory Execution Loop

For EVERY Wave, execute the following strict loop:

```text
OBSERVE -> UNDERSTAND -> PLAN -> IMPLEMENT -> VERIFY -> REVIEW -> RECORD -> NEXT
```

1. **OBSERVE**: Inspect `git status`, current workspace state, and verify clean build status.
2. **UNDERSTAND**: Read `AGENTS.md`, `docs/roadmap/CURRENT_STATE.md`, `docs/roadmap/ROADMAP.md`, and relevant specs/ADRs.
3. **PLAN**: Select a single uncompleted Wave. State its Goal, Scope, Acceptance Criteria, Risks, and Verification plan before modifying files.
4. **IMPLEMENT**: Make the minimal, cohesive code/doc diff necessary for this Wave.
5. **VERIFY**: Run build, unit tests, static analysis (lint/detekt), and relevant UI/instrumentation tests. Never claim a test passed without running it.
6. **REVIEW**: Self-review `git diff` against Acceptance Criteria and Architecture Invariants. Verify no regression risk.
7. **RECORD**: Update `docs/roadmap/CURRENT_STATE.md` with execution results and evidence.
8. **NEXT**: Automatically proceed to the next uncompleted Wave if all Quality Gates passed, or halt if BLOCKED / Stop Condition encountered.

---

## 3. Definition of Done (DoD)

A Wave is `DONE` if and only if:
```text
AcceptanceCriteriaSatisfied
  AND BuildGreen
  AND RelevantTestsGreen
  AND StaticAnalysisGreen
  AND RelevantUXChecksPassed
  AND DiffReviewed
  AND DocumentationUpdated
  AND ExecutionStateUpdated
```
If any condition evaluates to `false`, the Wave is **NOT DONE**.

---

## 4. Failure & Recovery Policy

When encountering build failure, test failure, or unexpected errors:
1. Follow: **Observe -> Hypothesis -> Reproduce -> Root Cause -> Fix -> Regression Verification**.
2. **FORBIDDEN**:
   - Deleting tests to achieve green status.
   - Disabling lint/detekt rules to bypass errors.
   - Adding unapproved dependencies to work around problems.
   - Performing destructive git operations (e.g. `git reset --hard` without explicit request).
   - Committing sensitive data, credentials, or `local.properties`.

---

## 5. Stop Conditions (BLOCKED)

Stop execution immediately and mark state as `BLOCKED` in `docs/roadmap/CURRENT_STATE.md` if:
- Specifications are contradictory.
- Architecture Decision Record (ADR) changes are required.
- Destructive data migration is necessary.
- Security/Privacy critical decisions arise.
- Major new third-party dependency is required.
- Official Android documentation directly contradicts present design.
- Verification environment / test harness itself is broken.

---

## 6. Repository Documentation Structure

- Constitution & Rules: `AGENTS.md`
- Product Specs: `docs/product/` (`PRODUCT.md`, `USER_JOURNEYS.md`, `NON_GOALS.md`)
- UX & Design: `docs/ux/` (`UX_PRINCIPLES.md`, `LEARNING_LOOP.md`, `SCREEN_SPEC.md`, `ACCESSIBILITY.md`)
- Architecture: `docs/architecture/` (`ARCHITECTURE.md`, `DATA_MODEL.md`, `MODULES.md`)
- Quality Strategy: `docs/quality/` (`TEST_STRATEGY.md`, `QUALITY_GATES.md`, `PERFORMANCE.md`)
- Roadmap & State: `docs/roadmap/` (`ROADMAP.md`, `CURRENT_STATE.md`, `BACKLOG.md`)
- Decision Records: `docs/adr/` (`ADR-XXXX-title.md`)
- Custom Agent Skills: `.agents/skills/` (`execute-wave`, `verify-change`, `ux-review`, `architecture-review`)
