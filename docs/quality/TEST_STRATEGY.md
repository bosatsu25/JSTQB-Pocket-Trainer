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
