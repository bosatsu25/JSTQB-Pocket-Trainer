# Learning Loop UX Specification

The **Critical Learning Loop** is the heart of TestReason. Every interaction within this loop must be polished and frictionless.

```text
+-----------------------+
|  1. Question Screen   |  <-- Clear typography, Choice radio buttons, Confidence selector
+-----------------------+
            │
            ▼
+-----------------------+
| 2. Answer Evaluation  |  <-- Instant visual result banner (Correct / Incorrect)
+-----------------------+
            │
            ▼
+-----------------------+
| 3. Explanation View   |  <-- Correct choice logic + Distractor breakdown per choice
+-----------------------+
            │
            ▼
+-----------------------+
| 4. Mistake Tagging    |  <-- If incorrect: Select mistake reason (Optional tag)
+-----------------------+
            │
            ▼
+-----------------------+
|  5. Result & Action   |  <-- Mastery updates, Score, Recommended Next Action CTA
+-----------------------+
```

---

## Confidence Selector Options
- **GUESS** (Pure guess)
- **LOW** (Unsure)
- **MEDIUM** (Fairly certain)
- **HIGH** (Sure of answer)
- **UNSPECIFIED** (Default state prior to user selection)

*Rules*:
1. The default state for confidence selection is **UNSPECIFIED** (`null` in data model). The system does NOT automatically record MEDIUM unless explicitly selected by the user.
2. Answering a question does NOT require selecting a confidence rating. Users can submit answers with confidence set to `UNSPECIFIED`.
3. If an answer is correct with `GUESS`, `LOW`, or `UNSPECIFIED` confidence, the spaced review scheduler queues the item for earlier review.

---

## Mistake Reason Tags (Optional)
1. **Concept Unclear** (Didn't understand the underlying JSTQB principle)
2. **Terminology Confusion** (Confused JSTQB terms e.g., error vs flaw vs fault)
3. **Misread Question** (Overlooked negative wording e.g., "NOT true")
4. **Careless Error** (Selected wrong radio button or rushed)

*Rule*: Mistake reason tagging is strictly optional. Users can proceed directly to the next question without selecting a tag.

---

## Review Scheduling (`SpacedReviewEngine`)
Each answer sets the question's next review date. The mistake tag is applied when the learner selects it
(`QuizRepository.updateMistakeReason` re-schedules the wrong answer).

| Answer | Interval |
|---|---|
| Incorrect, tagged CONCEPT / TERMINOLOGY or untagged | 1 day |
| Incorrect, tagged MISREAD | 2 days |
| Incorrect, tagged CARELESS | 3 days |
| Correct with GUESS | 1 day (treated as not yet learned) |
| Correct, first time or after a reset | HIGH 7 / MEDIUM 3 / LOW or UNSPECIFIED 1 day |
| Correct again | previous × 2.5 (HIGH) / × 2 (MEDIUM) / × 1.5 (LOW, UNSPECIFIED), at least +1 day, max 60 days |

A miss resets the interval regardless of history.

## Weak Questions
A question is weak while its **latest** answer was wrong or was correct with GUESS / LOW confidence.
A later confident correct answer removes it from the weakness queue.

## Mastery
Mastery per LO is the average credit per answer: correct HIGH/MEDIUM = 1.0, correct UNSPECIFIED = 0.75,
correct LOW = 0.5, correct GUESS = 0.25, incorrect = 0.

## Session Composition
- **Daily (5 questions)**: due reviews (oldest first) → weak questions (most recent first) → never-answered
  questions → random questions. No duplicates.
- **Review**: all due questions. **Weakness**: up to 5 weak questions.
- When a queue is empty no session is created; the UI says there is nothing to study in that mode.
