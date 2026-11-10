# Test Strategy: TestReason

---

## 1. Risk-Based Testing Pyramid

TestReason applies a risk-based testing approach:

```text
       / \
      /   \      E2E / Critical User Journey Tests (UI Automator / Compose UI Test)
     /     \
    /-------\    Integration Tests (Room DB, DAOs, Repositories)
   /         \
  /-----------\  Unit Tests (ViewModels, Mastery Computation, Review Engine, UseCases)
```

1. **Unit Tests (High Volume)**:
   - Target: Domain models, ViewModels, Mastery Engine, Review Scheduler logic.
   - Tools: JUnit 4/5, `kotlinx-coroutines-test`, Turbine (for Flow testing), Truth/AssertJ.

2. **Integration Tests (Medium Volume)**:
   - Target: Room DAOs, DataStore, Repository implementations.
   - Tools: AndroidX Test, Room In-Memory Database tests.

3. **Compose UI Tests (Targeted Volume)**:
   - Target: Key Compose components, screens (`HomeScreen`, `QuizScreen`, `ExplanationScreen`).
   - Tools: Compose UI Test (`createComposeRule`).

4. **E2E / CUJ Tests**:
   - Target: Critical Learning Loop (`Home -> Quiz -> Answer -> Explanation -> Result`), mock exam, drill, glossary.
   - Tools: Compose UI Test on Robolectric against the real app (section 4). No device tests yet.

---

## 2. Responsive Layout Tests (Compose UI on Robolectric)

Every screen exposes a stateless `*Content(uiState, callbacks)` composable. `app/src/test/.../ui/` renders each
one on the JVM for five device configurations (`TestDevice`):

| Device | Size (dp) | Font scale |
|---|---|---|
| SMALL_PHONE | 320 × 568 | 1.0 |
| SMALL_PHONE_FONT_200 | 320 × 568 | 2.0 |
| PHONE | 411 × 891 | 1.0 |
| PHONE_LANDSCAPE | 891 × 411 | 1.0 |
| TABLET_LANDSCAPE | 1280 × 800 | 1.0 |

Checks per screen (`ResponsiveTestSupport.kt`):
- **No horizontal overflow**: every semantics node lies within the screen width (unclipped geometry; content inside
  intentionally horizontal-scrolling tables/code is exempt).
- **Touch targets ≥ 48dp** for every clickable node (layout size, so off-screen items are checked too).
- **Primary actions reachable** with `performScrollTo()`.
- **Two-pane layouts** (quiz, explanation) at ≥ 720dp width; stacked below that.

Rules:
- Tests run with `@GraphicsMode(NATIVE)`: in LEGACY mode text has almost no width and overflow is invisible.
- `ResponsiveHarnessSelfTest` proves the assertions fail on a broken layout; keep it green.
- New screens must add a `*Content` composable and a case in a `*ResponsiveTest` before they are DONE.
- Use worst-case fixtures (long Japanese text, wide tables, 5-choice multi-answer questions) — see `ScreenFixtures`.

---

## 3. ViewModel Tests

- ViewModels are tested against the **real** `QuizRepositoryImpl` / `MasteryRepositoryImpl` on an in-memory Room
  database (`feature/*/src/test/.../testing/TestEnvironment.kt`), so behaviour such as session ordering or review
  scheduling is exercised end to end below the UI.
- `MainDispatcherRule` sets an `UnconfinedTestDispatcher` as Main; one-off events are asserted with Turbine.
- Time is controlled with `MutableClock` (the app's `TimeProvider`).
- Endless periodic flows must not run on the collector's dispatcher (the due-queue clock uses `flowOn(Default)`);
  otherwise `runTest` keeps advancing virtual time forever.

---

## 4. End-to-End Journey Tests (the real app on the JVM)

`app/src/test/.../e2e/` starts `MainActivity` on Robolectric with the **real** Hilt graph, the real Room
database and the bundled questions and glossary. Nothing is faked, so these tests cover what the per-screen
tests cannot: that the dependency graph starts, that every screen is wired into `TestReasonNavHost`, and that
navigation, the back stack and state restoration behave. No test-only dependency is needed (no
`hilt-android-testing`): the tests run the production `TestReasonApplication`.

| Class | What it covers |
|---|---|
| `AppJourneyTest` | daily session Home → Quiz → Explanation → Result → Home; back from the explanation and resume; mock exam left, resumed and submitted; rapid drill; glossary and cards; review / analytics / chapter select |
| `ProcessRecreationTest` | the activity is destroyed and rebuilt from its saved instance state only (new ViewModels): quiz selection and confidence, mock exam answers and clock, drill lap and shown answer |

Rules:
- `AppRobot` drives the app by the texts on screen and waits for asynchronous loads; on a timeout it reports what
  was on screen. Never replace a wait with a fixed sleep (the only sleep is the 400 ms tap guard of the drill and
  the cards, which runs on the real clock here).
- The content is real and the composition random (which questions, single or two answers). A journey that fails
  only sometimes has found a defect that depends on the content: reproduce it by re-running, do not retry it away.
  (That is how the mock exam's lost-selection race was found.)
- Time cannot be moved in these tests (`SystemTimeProvider`). Deadlines and review intervals stay with the
  ViewModel and repository tests, which control the clock.
- Simulated process death keeps the process, so singletons (database, repositories) are the same objects.
- Every class that launches a real activity uses `DrainMainLooperRule` as its outermost rule. All tests of a
  module share one JVM, and a main-looper task left behind by one test stops later Compose tests from ever
  becoming idle.
- A new screen or route adds a step to a journey before it is DONE.
- **Not covered**: a real device or emulator (rendering on real hardware, TalkBack, system back gesture,
  performance). That stays manual until an instrumented test run exists.

