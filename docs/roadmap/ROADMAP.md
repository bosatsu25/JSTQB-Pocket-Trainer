# TestReason Product Roadmap

---

## Milestone M0 — Harness & Specification
- **Wave 0.1: Harness Bootstrap**
  - **Goal**: Establish repository constitution, specs, quality gates, roadmap, ADR baseline, and agent skills.
  - **Status**: COMPLETED

---

## Milestone M1 — Working Learning Loop with Persistence
- **Wave 1.1: Gradle & Version Catalog Setup**
  - **Goal**: Configure root build files, Version Catalog (`libs.versions.toml`), Kotlin, Compose compiler.
  - **Status**: COMPLETED
- **Wave 1.2: Multi-Module Structure & App Shell**
  - **Goal**: Create core and feature module hierarchy, Application class, MainActivity shell.
  - **Status**: COMPLETED
- **Wave 1.3: Design System & Theme Foundation**
  - **Goal**: Set up Material 3 theme, colors, typography, and common UI components in `core:ui`.
  - **Status**: COMPLETED
- **Wave 1.4: Navigation Setup**
  - **Goal**: Configure Navigation host and type-safe routes (`Screen.Home`, `Screen.Quiz`, `Screen.Explanation`, `Screen.Result`).
  - **Status**: COMPLETED
- **Wave 2.1: Domain Models & Enums**
  - **Goal**: Implement `Question`, `Choice`, `Attempt`, `QuizSession`, `Mastery`, `LearningObjective`.
  - **Status**: COMPLETED
- **Wave 3.1 - 3.6: Critical Learning Loop Vertical Slice**
  - **Goal**: Complete Home -> Quiz -> Answer -> Explanation -> Result -> Recommended Next Action flow.
  - **Status**: COMPLETED
- **Wave 4.1 - 4.3: Room & DataStore Persistence**
  - **Goal**: Implement Room database (`testreason.db`), DataStore preferences, and repositories.
  - **Status**: COMPLETED

---

## Milestone M2 — Review Intelligence, Analytics & Study Modes
- **Wave 5.1: Spaced Review Engine**
  - **Goal**: Calculate review queues based on mistake tags and confidence levels.
  - **Status**: IN_PROGRESS
- **Wave 5.2: Mastery & LO Analytics Dashboard**
  - **Goal**: Visualize LO mastery levels and weakness areas.
  - **Status**: TODO
- **Wave 6.1: Study Modes (Chapter & Weakness Review)**
  - **Goal**: Implement Chapter Study and Targeted Weakness Review sessions.
  - **Status**: TODO

---

## Milestone M3 — Mock Exam Mode
- **Wave 7.1: Timed Mock Exam Engine**
  - **Goal**: Implement 40-question timed exam session, navigator, flag for review, and pass/fail scoring.
  - **Status**: TODO

---

## Milestone M4 — UX Polish, Performance & Release Readiness
- **Wave 8.1: Accessibility & Adaptive Polish**
  - **Goal**: Audit TalkBack descriptions, touch target sizes, font scaling, and dynamic color styling.
  - **Status**: TODO
- **Wave 9.1: Performance & Macrobenchmarks**
  - **Goal**: Generate Baseline Profile and measure startup/scroll benchmarks.
  - **Status**: TODO
- **Wave 10.1: Release Hardening & APK Verification**
  - **Goal**: Perform legal, content verification, privacy audits, and prepare release build.
  - **Status**: TODO
