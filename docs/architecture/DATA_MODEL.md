# Data Model Specification: TestReason

---

Room schema version: **2** (migration 1→2 adds the question columns; see `Migrations.kt`).

## 1. Storage Boundaries & Responsibilities

1. **Room Database**: Single Source of Truth for persistent domain entities:
   - `Question`, `QuestionVersion`, `Choice`, `Attempt`, `QuizSession`, `ReviewSchedule`, `Mastery`.
2. **DataStore Preferences**:
   - Theme settings (Light / Dark / System), daily target goal, notification preferences.
3. **SavedStateHandle / UI State**:
   - Temporary screen interaction state, current question index, navigation arguments.

---

## 2. Domain Entities & Value Objects

### `LearningObjective` (LO)
Static master in `core/model/Syllabus.kt` (64 LOs of syllabus 2023V4.0.J02), not stored in Room.
- `id`: String (e.g., `"FL-1.1.1"`); `section` and `chapter` are derived from it
- `kLevel`: KLevel (K1, K2, K3)
- `summary`: String (our own wording, see ADR-0006)

### `Question`
Bundled as JSON in `core/src/main/assets/questions/` (format: `docs/content/AUTHORING_GUIDE.md`) and
synchronised into Room on start-up by revision (`QuizRepository.syncBundledQuestions`).
- `id`: String (`c{chapter}-{nnn}`, immutable)
- `learningObjectiveId`: String
- `stem`: String (Question body text)
- `scenario`: String? (specification / table / code shown above the stem)
- `choices`: List<Choice>
- `selectCount`: Int (1, or 2 for 「2つ選べ」; grading requires the exact correct set)
- `explanation`: String
- `revision`: Int (content version; a higher bundled revision overwrites the stored row)
- `isActive` (Room only): retired questions are kept for attempt history but never served

### `Choice`
- `id`: String (e.g., `"A"`, `"B"`, `"C"`, `"D"`)
- `text`: String
- `isCorrect`: Boolean
- `distractorAnalysis`: String (Why it's right/wrong)

### `Attempt`
- `id`: String
- `sessionId`: String
- `questionId`: String
- `selectedChoiceIds`: List<String> (Room column `selectedChoiceId` stores them sorted and comma-joined)
- `isCorrect`: Boolean
- `confidence`: ConfidenceLevel? (`GUESS`, `LOW`, `MEDIUM`, `HIGH`, or `null`/`UNSPECIFIED`)
- `mistakeReason`: MistakeReason? (`CONCEPT`, `TERMINOLOGY`, `MISREAD`, `CARELESS`, or `null`)
- `timeSpentMs`: Long
- `timestamp`: Instant

### `QuizSession`
- `id`: String
- `mode`: StudyMode (`DAILY`, `CHAPTER`, `WEAKNESS`, `MOCK_EXAM`, `CUSTOM`)
- `startedAt`: Instant
- `completedAt`: Instant?
- `totalQuestions`: Int
- `isFinalized`: Boolean

### `Mastery`
- `learningObjectiveId`: String
- `score`: Float (0.0 to 1.0)
- `lastAttemptAt`: Instant
- `totalAttempts`: Int
- `correctAttempts`: Int
