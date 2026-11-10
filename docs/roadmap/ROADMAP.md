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
Tracking epic: GitHub issue #1 (dependency-ordered checklist of #2–#25).

- **Wave 4.4: Content Foundation (#2 CI, #3 ADR-0006, #4 LO master, #5 question format v2, #6 bundled JSON pipeline, #7 sample LO fix)**
  - **Goal**: CI on JDK 17, copyright policy, 64-LO master, multi-answer/scenario questions, revision-based content sync with validation.
  - **Status**: COMPLETED
- **Wave 4.5: Syllabus Question Bank (#8–#13)**
  - **Goal**: Original questions for all 64 LOs (K1/K2 ≥ 3, K3 ≥ 6 per LO), independently reviewed, no copied syllabus text.
  - **Status**: COMPLETED (229 questions)
- **Wave 5.1: Spaced Review Engine**
  - **Goal**: Calculate review queues based on mistake tags and confidence levels.
  - **Status**: COMPLETED (#14 scheduling/weakness/mastery, #15 empty sessions, live clock, daily composition)
- **Wave 5.1b: Navigation & Home (#16)**
  - **Goal**: Review and analytics reachable from Home, resume card, result CTA starts the next session.
  - **Status**: COMPLETED
- **Wave 5.2: Mastery & LO Analytics Dashboard**
  - **Goal**: Visualize LO mastery levels and weakness areas.
  - **Status**: COMPLETED (#18)
- **Wave 6.1: Study Modes (Chapter & Weakness Review)**
  - **Goal**: Implement Chapter Study and Targeted Weakness Review sessions.
  - **Status**: COMPLETED (#17; weakness sessions in #14/#16)
- **Wave 6.2: Responsive Layout & Screen Spec Alignment (#20)**
  - **Goal**: Every screen verified on 5 device configurations; result reflection card; confidence rule aligned.
  - **Status**: COMPLETED

---

## Milestone M3 — Mock Exam Mode
- **Wave 7.1: Timed Mock Exam Engine**
  - **Goal**: Implement 40-question timed exam session, navigator, flag for review, and pass/fail scoring.
  - **Status**: COMPLETED (#19; blueprint per ISTQB v1.18, see docs/product/EXAM_FORMAT.md)
- **Wave 7.0: ViewModel Test Coverage (#22)**
  - **Status**: COMPLETED

---

## Milestone M4 — UX Polish, Performance & Release Readiness
Recommended order (epic #1): #27 → #28 → #32 → #4 → #30 → #29 → #23 → #21 → #24 → #25.

- **Wave 7.2: Content Human Review (#27)**
  - **Goal**: Human review of the 52 K3 questions and a K1/K2 spot-check; required before release.
  - **Status**: TODO
- **Wave 7.3: Double-tap Guards for Session Start (#28)**
  - **Goal**: Home / result / chapter / analytics / mock-exam start buttons cannot create duplicate sessions.
  - **Status**: COMPLETED
- **Wave 7.5: Rapid Drill Mode (#32)**
  - **Goal**: 周回モード — answer shown on the tap, missed questions repeat until the lap is cleared; not recorded in mastery / review.
  - **Status**: COMPLETED
- **Wave 7.4a: CI Stability (#30)**
  - **Goal**: No dependency-download failures on PR runs; no Node 20 deprecation warnings.
  - **Status**: COMPLETED (actions moved to Node 24 versions; owner still to confirm the warning is gone and to make `verify` a required check)
- **Wave 7.4b: Navigation E2E Tests (#29)**
  - **Goal**: Journeys through the real app (Hilt graph, Room, bundled content) on the JVM, including simulated process death.
  - **Status**: COMPLETED (found and fixed: mock exam lost a choice on two quick taps; quiz input was not restored after process death)
- **Wave 8.1: Accessibility & Adaptive Polish**
  - **Goal**: Audit TalkBack descriptions, touch target sizes, font scaling, and dynamic color styling.
  - **Status**: IN_PROGRESS (#23 — touch targets, 200% font, landscape/tablet, non-colour cues, names/roles, table rows and the exam clock warning done at the semantics level; TalkBack, Accessibility Scanner and dynamic colour need a device)
- **Wave 8.2: Settings (#21)**
  - **Status**: TODO
- **Wave 8.3: Chapter Keywords & Glossary Cards (#4)**
  - **Goal**: All 94 syllabus keywords with our own one-line summaries; glossary list and flash cards; keyword coverage report.
  - **Status**: COMPLETED
- **Wave 9.1: Performance & Macrobenchmarks**
  - **Goal**: Generate Baseline Profile and measure startup/scroll benchmarks.
  - **Status**: TODO (#24 — clock moved off the main thread; N+1 queries and measurements remain)
- **Wave 10.1: Release Hardening & APK Verification**
  - **Goal**: Perform legal, content verification, privacy audits, and prepare release build.
  - **Status**: TODO (#25; depends on #27)
