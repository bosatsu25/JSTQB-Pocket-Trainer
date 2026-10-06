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
