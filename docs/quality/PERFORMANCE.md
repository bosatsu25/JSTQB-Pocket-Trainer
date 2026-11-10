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

---

## 4. Database Access
- A list of questions is loaded with **one query**, never one query per question
  (`QuestionDao.getQuestionsByIds` through `getQuestionsInOrder`, which keeps the requested order and splits the
  ids into chunks below SQLite's 999-variable limit of older devices).
- `QuestionQueryCountTest` counts the queries: the mock exam (reloaded on every tap) went from 40 question
  queries per load to 1; the weak and due lists and the session result likewise use one.
- Bundled questions are read and parsed on `Dispatchers.IO` and written in one Room transaction; the sync runs
  once per process.

## 5. Not Measured Yet (needs a device)
- Cold start time, frame times and a Baseline Profile (issue #24). Nothing in this document has been measured on
  a device; the query counts above are the only performance numbers that exist.
- Whether starting the question sync from `Application` instead of the first ViewModel is worth it: decide after
  measuring the start-up, not before.

