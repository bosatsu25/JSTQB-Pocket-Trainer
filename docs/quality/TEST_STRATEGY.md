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
   - Target: Critical Learning Loop (`Home -> Quiz -> Answer -> Explanation -> Result`).

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
