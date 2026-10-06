# Performance Strategy: TestReason

---

## 1. App Startup Performance
- Target cold startup time: `< 1000ms` on mid-range devices.
- Measure via Macrobenchmark startup tests.
- Utilize Baseline Profiles to pre-compile critical execution paths.

---

## 2. UI Rendering & Jank Prevention
- Target frame rate: Constant 60fps / 120fps without frame drops during scroll and transition animations.
- Use Compose `Modifier` best practices (avoid unnecessary recompositions, use `@Stable` / `@Immutable` annotations, prefer derivedStateOf where appropriate).

---

## 3. Process Death & State Restoration
- ViewModels and Screens must properly restore state across activity recreation (e.g. screen rotation or background process death) using `SavedStateHandle`.
