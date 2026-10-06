# Architecture Specification: TestReason

---

## 1. Architectural Overview

TestReason follows **Modern Android Architecture** principles with a pragmatic Clean Architecture layering:

```text
+-------------------------------------------------------+
|                       UI Layer                        |
|  Compose Screens + ViewModels (UDF: State Down, Event Up) |
+-------------------------------------------------------+
                           │
                           ▼
+-------------------------------------------------------+
|                    Domain Layer                       |
|   Pure Kotlin Models + UseCases (Only when complex)   |
+-------------------------------------------------------+
                           │
                           ▼
+-------------------------------------------------------+
|                     Data Layer                        |
| Repositories (SSOT) -> Room DB + DataStore Preferences |
+-------------------------------------------------------+
```

---

## 2. Unidirectional Data Flow (UDF)

- **State Down**: ViewModels expose immutable `StateFlow<UiState>` to Compose UI screens.
- **Events Up**: UI components trigger events via explicit lambdas or `onEvent(UiEvent)` calls to the ViewModel.
- Side effects (e.g., navigation events, snackbars) are handled via standard StateFlow / SharedFlow one-time events.

---

## 3. Single Source of Truth (SSOT) & Offline-First

- **Room Database** is the Single Source of Truth for domain entities (`Question`, `Attempt`, `QuizSession`, `ReviewSchedule`, `Mastery`).
- **DataStore** stores preferences, user settings, and current daily goal configurations.
- All repository read APIs return reactive `Flow<T>` streams observing local database changes.
