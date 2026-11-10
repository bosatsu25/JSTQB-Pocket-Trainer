# Current Execution State: TestReason

---

## State Snapshot

- **Current Milestone**: Milestone M4 - IN_PROGRESS (M2–M3 merged to main in PR #26)
- **Current Branch**: `claude/vibrant-maxwell-2sx0ab`
- **Tracking**: GitHub epic #1 (issues #2–#32)
- **Room Schema**: version 3 (non-destructive migrations 1→2, 2→3)
- **Question Bank**: 229 questions covering all 64 LOs of syllabus 2023V4.0.J02; glossary of all 94 chapter keywords
- **Module Architecture**: 5 Modules (`:app`, `:core`, `:feature:study`, `:feature:review`, `:feature:progress`)
- **Integrity Gate / Build Foundation**: Final Cleanup Completed
- **Known Blockers**: None for this wave. Nothing has run on a device or emulator (no AVD in CI/container); journeys run on the JVM.

---

## Acceptance Matrix (Build Foundation & Quality Gates)

| Acceptance Criterion | Verification Method | Status | Evidence |
| :--- | :--- | :--- | :--- |
| **5-Module Consolidation** | Gradle Settings & Module Build Files | **PASS** | Consolidated 13+ modules down to 5 modules (`:app`, `:core`, `:feature:study`, `:feature:review`, `:feature:progress`) |
| **Room Baseline Schema v1** | `TestReasonDatabase` `version = 1` | **PASS** | `1.json` exported containing all 6 entities (`Question`, `Attempt`, `QuizSession`, `Mastery`, `ReviewSchedule`, `QuestionCompletion`) for initial release |
| **`completeQuestion` Production Transaction & Rollback** | `RepositoryRollbackTest.completeQuestion_rollbackRevertsCompletionAndSessionIndexWhenSessionUpdateFails` | **PASS** | Direct `QuizRepositoryImpl` call with SQLite trigger failure verifies production `db.withTransaction` rolls back completion, mastery, and session index |
| **`finalizeAttempt` Production Transaction & Rollback** | `RepositoryRollbackTest.finalizeAttempt_rollbackRevertsAttemptWhenScheduleFails` | **PASS** | Direct `QuizRepositoryImpl` call with SQLite trigger failure verifies production `db.withTransaction` rolls back attempt and review schedule |
| **Domain Invariant: Future Question Advance Rejection** | `QuizRepositoryTest.completeQuestion_preventsAdvancingToFutureQuestions` | **PASS** | `completeQuestion` on future/unreached `questionId` is rejected without advancing index or updating mastery |
| **`completeQuestion` Idempotency** | `QuizRepositoryTest.completeQuestion_isIdempotentAndUpdatesMasteryOnce` | **PASS** | `QuestionCompletionEntity` + `insertCompletionIgnore` ensures mastery updated ONLY ONCE |
| **First Answer Immutable** | `QuizRepositoryTest.finalizeAttempt_resubmissionPreservesFirstAnswerAndChoice` | **PASS** | `insertAttemptIgnore` preserves original choice & correctness on duplicate submissions |
| **Mistake Reason Isolated Update** | `AttemptDao.updateMistakeReasonOnly` | **PASS** | `UPDATE attempts SET mistakeReason = :mistakeReason` updates tag without re-scoring |
| **StudyMode `REVIEW` & Deterministic Due Order** | `ReviewScheduleDao.getDueReviewSchedules` | **PASS** | `StudyMode.REVIEW` used; `ORDER BY nextReviewAt ASC, questionId ASC` returns oldest overdue questions first |
| **Due Review Queue Boundary (`q_unseen` Excluded)** | `QuizRepositoryTest.observeDueReviewQuestions_includesDueAndNow_excludesFutureAndUnseen` | **PASS** | `q_due` & `q_now` included, `q_future` & `q_unseen` (`q_fl_2_2_1`) excluded |
| **File-Backed Room DB Persistence** | `FileRoomPersistenceTest.fileDatabase_savesAndRestoresCompleteStateAcrossDbReopen` | **PASS** | DB closed & reopened from disk; session, attempts, schedules, completions restored |
| **Android Lint Check** | `gradlew.bat lintDebug` | **PASS** | 0 errors across 5 modules |
| **Strict JDK 17 Validation** | `scripts/verify.sh` (Linux, JDK 17.0) + GitHub Actions `ci.yml` (Temurin 17) | **PASS** | `assembleDebug testDebugUnitTest lintDebug detekt` green locally on JDK 17; CI runs the same tasks on every PR. `verify.ps1` remains for Windows hosts |
| **Navigation 2.8.5 Type-Safe Routes** | `AppNavHost.kt` + `kotlinx.serialization` | **PASS** | Maintained stable Type-Safe Navigation Compose 2.8.5 |
| **Device Verification** | Physical Device / Emulator | **NOT_RUN** | No connected ADB device / AVD in CLI env. Journeys run on the JVM instead (see E2E Journeys below) |

| **LO Master (64 LOs)** | `SyllabusTest` | **PASS** | 64 LOs, per-chapter counts 14/10/8/14/16/2, eight K3 LOs, 1135 minutes |
| **Room v1→v2 Migration** | `MigrationTest.migrate1To2_keepsQuestionsAndAttemptsAndAddsDefaults` | **PASS** | `MigrationTestHelper.runMigrationsAndValidate` against exported `1.json`/`2.json`; existing rows and attempts preserved |
| **Bundled Content Sync** | `BundledQuestionSyncTest` (4 tests) | **PASS** | New questions reach existing DBs, revision bump updates text, removed/retired questions deactivated, empty bundle never retires the bank |
| **Multi-answer Grading** | `QuestionGradingTest` | **PASS** | Exact-set grading; attempt selection round-trips through the entity |
| **Scenario Rendering Parser** | `ScenarioParserTest` | **PASS** | Paragraphs, Markdown tables (incl. aligned separators) and code blocks |
| **Question Bank Validation** | `QuestionContentValidationTest` | **PASS** | Schema rules, LO ids/chapters, K3 scenarios, banned terminology, per-LO minimum (K1/K2 ≥ 3, K3 ≥ 6) |
| **Review Scheduling (#14)** | `SpacedReviewEngineTest` (5), `ReviewIntelligenceTest.mistakeReason_decidesTheNextReviewInterval` | **PASS** | Mistake tag re-schedules (CARELESS 3d, CONCEPT 1d); HIGH streak 7→18→45→60; miss/guess resets |
| **Weakness & Mastery (#14)** | `ReviewIntelligenceTest.weakQuestion_isClearedByALaterConfidentCorrectAnswer`, `MasteryRepositoryTest` (2) | **PASS** | Latest-attempt weakness; confidence-weighted mastery via `TimeProvider` |
| **Empty Sessions / Live Clock / Daily Mix (#15)** | `ReviewIntelligenceTest` (3 more tests) | **PASS** | No empty sessions; due queue follows the clock on the same Flow; daily = due → weak → unseen |
| **Navigation & Home (#16)** | `HomeUiStateTest`, `ChapterProgressTest`, `assembleDebug` | **PASS** | Review/Analytics routes, resume card, result CTA; no empty `onClick` left. UI not exercised on a device |
| **Responsive Layout (5 devices)** | `StudyScreensResponsiveTest`, `ProgressScreensResponsiveTest` (40 cases), `ResponsiveHarnessSelfTest` (3) | **PASS** | Home, Quiz, Explanation, Result, Chapter Select, Review (2 states), Analytics on 320×568, 320×568 @font 2.0, 411×891, 891×411, 1280×800: no horizontal overflow, touch targets ≥ 48dp, actions reachable, two panes ≥ 720dp |
| **Result Reflection (#20)** | `SessionSummaryTest` | **PASS** | Time, confidence × correctness, strong vs review LOs |
| **Chapter Study (#17)** | `ChapterSessionTest`, `ChapterOverviewTest` | **PASS** | Chapter-only sessions ordered unseen → weak → oldest |
| **LO Dashboard (#18)** | `DashboardTest`, `LearningObjectiveSessionTest` | **PASS** | K-level accuracy, mistake breakdown, 64 LO states, LO sessions |
| **ViewModel Tests (#22)** | 7 ViewModel test classes in feature modules (17 tests) | **PASS** | Real repositories on in-memory Room; found and fixed the due-queue clock spinning on the collector's dispatcher |
| **Exam Blueprint (#19)** | `ExamBlueprintTest`, `ExamComposerTest` | **PASS** | 40 questions, K1/K2/K3 = 8/24/8, chapters 8/6/4/11/9/2, 29 LO groups covering all 64 LOs (ISTQB v1.18); bundled bank supports 3 exams without repeats |
| **Mock Exam Flow (#19)** | `MockExamRepositoryTest` (6), `MockExamViewModelTest` (6), `MigrationTest.migrate2To3…`, `MockExamResponsiveTest` (20) | **PASS** | Answers changeable until submit, deadline enforced, auto-submit and post-restart submit, 25 = fail / 26 = pass, only answered questions recorded |
| **Session Start Guards (#28)** | Home / Result / ChapterSelect / Analytics / Review / MockExam ViewModel tests, `MockExamRepositoryTest.onlyOneExamCanBeOpen…`, `MockExamResponsiveTest.introWithAnOpenExam…` | **PASS** | Second tap ignored until the screen is shown again; one open mock exam at a time. Mutation check: removing the lock in Home and ChapterSelect makes their tests fail |
| **Rapid Drill (#32)** | `DrillLapTest` (4), `DrillViewModelTest` (15), `DrillScreensResponsiveTest` (30) | **PASS** | Answer shown on the tap, misses re-queued until the lap is cleared, lap restored from saved state (shown answer, finish time, retired questions dropped); a finished lap leaves attempts / mastery / weak / due / sessions empty; a double tap on "次へ" does not answer the next question; each question starts at the top (mutation-checked). UI not exercised on a device |
| **Glossary (#4)** | `GlossaryContentValidationTest` (3), `GlossaryViewModelTest` (5), `GlossaryScreensResponsiveTest` (25) | **PASS** | 94 keywords, per-chapter counts 28/17/10/18/25/1, one line ≤ 60 chars, banned terminology; 91/94 keywords mentioned by the question bank (ratchet on the other 3); cards re-queue まだ, double tap cannot grade. 14-char overlap with the syllabus: 3 runs, all bare term enumerations. UI not exercised on a device |
| **E2E Journeys (#29)** | `AppJourneyTest` (6), `ProcessRecreationTest` (3), `MockExamRepositoryTest.selectChoice…` (2), `MockExamViewModelTest.twoQuickTaps…`, `QuizViewModelTest.selectionAndConfidence_surviveProcessDeath…` | **PASS** | Real Hilt graph, Room and bundled content on Robolectric; 6 consecutive runs green. Mutation checks: the old mock exam selection code fails the quick-tap test; removing the quiz restore fails the recreation journey |
| **Screen-reader Semantics (#23, partial)** | `assertAccessible` in all `*ResponsiveTest` classes, `ResponsiveHarnessSelfTest` (3 new), `ScenarioParserTest.tableRows…`, `MockExamResponsiveTest` (live region), `QuestionContentValidationTest` (table header rule) | **PASS** | Names and roles on every interactive element on 5 devices; table rows paired with headers; exam warning is a polite live region and the clock is not. Semantics level only: nothing was heard through TalkBack |
| **Settings (#21, partial)** | `DataStorePreferencesTest` (4, real DataStore file), `SettingsViewModelTest` (2), `HomeViewModelTest.dailyQuiz_usesTheDailyGoalSetting`, `StudyScreensResponsiveTest.settings`, `AppJourneyTest.settings…`, `ProcessRecreationTest.settings…` | **PASS** | Settings survive a restart; a 10-question goal gives a 10-question session through the real app; changing the theme redraws the app without losing the screen. The theme colours themselves were not looked at on a device |
| **Copyright Check (ADR-0006)** | Reviewer pass + 18-char overlap script against the syllabus text (not committed: the syllabus text is copyrighted) | **PASS** | Seven independent reviewers (not the authors) re-checked every key against the syllabus (K3 answers recomputed) and rewrote copied text; 18-char overlap hits went from 178 to 0 across all 8 files. Key changes: none. Wording fixes for ambiguity: c1-016, c2-025, c3-001; explanation fixes: c4-011, c4-016, c5-028 |

---

## Execution Log

- `claude/vibrant-maxwell-2sx0ab` (Wave 8.2, issue #21, without the reminder):
  - Settings screen: theme (system / light / dark) and daily goal (5 / 10 / 20). The DataStore code existed but nothing used it.
  - Home's daily session uses the goal; the activity applies the theme and draws nothing until it is read.
  - Unknown stored values fall back to defaults (the old code crashed on an unknown theme name).
  - Not done: the reminder notification (needs WorkManager and `POST_NOTIFICATIONS`; owner decision).
- `claude/vibrant-maxwell-2sx0ab` (Wave 8.1, issue #23, the part that needs no device):
  - Every screen test now also checks screen-reader names and roles (`assertAccessible`); no violation was found in the existing screens.
  - Scenario table rows are read as one sentence with their column headers; the mock exam's low-time warning is announced once.
  - Still open in #23: TalkBack, Accessibility Scanner and dynamic colour on a device.
- `claude/vibrant-maxwell-2sx0ab` (Wave 7.4b, issue #29):
  - End-to-end journey tests run the real app on the JVM (`AppJourneyTest`, `ProcessRecreationTest`); policy in `TEST_STRATEGY.md` §4.
  - Defect found by the journeys and fixed: in the mock exam, two quick taps on a two-answer question kept only the second choice (the selection was computed from screen state that lags behind the database). The repository now applies each tap to the saved answer in one transaction.
  - Gap against `PERFORMANCE.md` §3 fixed: the quiz selection and confidence are saved in `SavedStateHandle` and restored after process death.
- `claude/vibrant-maxwell-2sx0ab` (Wave 7.4a, issue #30):
  - CI actions on Node 24: `actions/checkout@v5`, `actions/setup-java@v5`, `actions/upload-artifact@v6`, `gradle/actions/setup-gradle@v5`. `setup-gradle@v6` is not adopted: its caching component has separate Terms of Use (owner decision).
  - Dependency downloads: no configuration change was needed. Once main had a CI run the Gradle cache became readable from PRs; the seven PR runs since then (runs 11, 12, 14, 15, 17, 18, 20) had no dependency failure and run time fell from about 6 to about 3 minutes.
- `claude/vibrant-maxwell-2sx0ab` (Wave 8.3, issue #4):
  - Glossary: 94 syllabus keywords with one-line summaries in our own words (authored, then independently reviewed against the syllabus: 50 summaries rewritten, keyword lists matched per chapter).
  - Glossary list with chapter filter, flash cards with the drill's lap rule, keyword coverage report (3 keywords are in no question text).
- `claude/vibrant-maxwell-2sx0ab` (Wave 7.5, issue #32):
  - Rapid drill (周回モード): setup by scope, answer revealed on the tap, missed questions re-queued until the lap is cleared, lap summary with pace.
  - Drill answers are deliberately not recorded (no attempt / mastery / review / session); reason in `LEARNING_LOOP.md`.
- `claude/vibrant-maxwell-2sx0ab` (Wave 7.3, issue #28):
  - Start buttons lock from the tap until the screen is shown again; the review screen had no guard at all.
  - Only one mock exam can be open; the intro offers resume instead of start while one is unsubmitted.
  - Review finding fixed: Home stays locked while a session is still being created, even if Home is shown again.
- PR #26 merged to main (merge commit 2161144): issues #2, #3, #5–#20, #22 closed.
- `claude/vibrant-maxwell-2sx0ab` (Waves 7.0 / 7.1, issues #22, #19):
  - Exam format researched from JSTQB exam guide, ISTQB Exam Structure and Rules Tables v1.18 and Pearson VUE (`docs/product/EXAM_FORMAT.md`); the published FL syllabus is still 2023V4.0.J02.
  - ViewModel tests for every screen; repository clock moved off the main dispatcher.
  - Mock exam: blueprint composer, Room v3 tables, timed exam with navigator/flags, auto-submit, results by chapter and K-level.
- `claude/vibrant-maxwell-2sx0ab` (Waves 5.2 / 6.1 / 6.2, issues #17, #18, #20 + responsive tests):
  - Responsive Compose UI test harness on Robolectric (NATIVE graphics) over 5 device configurations; fixed chip touch targets (~36×35dp → 48dp), wrapping menus/chips, width cap, two-pane quiz/explanation.
  - Result reflection card; confidence "not selected" rule aligned between SCREEN_SPEC and LEARNING_LOOP.
  - Chapter study mode and chapter select screen; LO dashboard with LO sessions.
- `claude/vibrant-maxwell-2sx0ab` (Wave 5.1 / 5.1b, issues #14–#16):
  - Spaced review: mistake tag re-schedules, intervals grow from the previous one, latest-attempt weakness, confidence-weighted mastery (spec in `LEARNING_LOOP.md`).
  - Sessions: empty queues create no session; due queue re-evaluated on a 1-minute clock; daily mix due → weak → unseen → random.
  - Home: resume card, review / weakness / analytics menu, snackbar for empty queues; Review and Analytics routes; result CTA starts the next session; minimal chapter progress screen.
- `claude/vibrant-maxwell-2sx0ab` (Waves 4.4 / 4.5, issues #2–#13):
  - Added CI (`.github/workflows/ci.yml`), Unix `gradlew`, `scripts/verify.sh`.
  - ADR-0006 content/trademark policy and `docs/content/AUTHORING_GUIDE.md`.
  - `Syllabus` LO master; question format v2 (multi-answer, scenario, revision, retirement) with Room migration 1→2.
  - Bundled question JSON pipeline with revision-based sync (replaces seed-only-when-empty) and content validation tests.
  - 229 original questions authored per chapter, then independently reviewed for correctness (K3 answers recomputed) and copied syllabus text removed.

- `refactor/build-foundation-simplification`:
  - Consolidated 13+ modules down to 5 modules (`:app`, `:core`, `:feature:study`, `:feature:review`, `:feature:progress`).
  - Reset Room database version to baseline `version = 1` containing all 6 domain entities.
  - Wrapped `QuizRepositoryImpl.completeQuestion` and `finalizeAttempt` inside `db.withTransaction`.
  - Added strict domain invariants enforcing current question completion order and attempt presence.
  - Added `StudyMode.REVIEW` and deterministic overdue review queue sorting (`ORDER BY nextReviewAt ASC, questionId ASC`).
  - Updated `scripts/verify.ps1` to strictly require JDK 17 and fail on non-JDK 17 runtimes without fallback.
