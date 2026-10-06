# Data Model Specification: TestReason

---

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
- `id`: String (e.g., `"FL-1.1.1"`)
- `chapterId`: Int
- `title`: String
- `description`: String
- `kLevel`: KLevel (K1, K2, K3)

### `Question`
- `id`: String
- `learningObjectiveId`: String
- `stem`: String (Question body text)
- `choices`: List<Choice>
- `explanation`: Explanation

### `Choice`
- `id`: String (e.g., `"A"`, `"B"`, `"C"`, `"D"`)
- `text`: String
- `isCorrect`: Boolean
- `distractorAnalysis`: String (Why it's right/wrong)

### `Attempt`
- `id`: String
- `sessionId`: String
- `questionId`: String
- `selectedChoiceId`: String
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
