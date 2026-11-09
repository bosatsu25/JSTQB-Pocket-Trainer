# Screen Specifications: TestReason

---

## 1. Home Screen (`HomeScreen`)

### Layout Components:
1. **Top Bar**: App Title ("TestReason").
2. **Mastery Card**: overall mastery (average of LO masteries).
3. **Resume Card** (only while an unfinished session has questions left): progress and "続きから再開".
4. **Hero Card**: "今日のおすすめ学習" — due → weak → unseen questions (5 Qs, ~3 mins).
5. **Learning Menu**:
   - 今日の復習 (due count) → Review screen
   - 弱点克服 (weak count) → starts a weakness session, or a message when there is none
   - 学習分析 → chapter-level progress (full LO dashboard: #18)
   - Chapter Study (#17) and Mock Exam (#19) are added when implemented; no placeholder buttons.

---

## 2. Quiz Session Screen (`QuizScreen`)

### Layout Components:
1. **Header / Progress Bar**:
   - Question X of Y indicator.
   - JSTQB Syllabus LO Tag (e.g. `[FL-1.1.2]`).
2. **Question Body**:
   - Rendered stem text with support for code snippets / formatted lists.
3. **Choice List**:
   - Single-choice radio cards (A, B, C, D).
4. **Confidence Selector** (optional):
   - 4 chips: 勘で選んだ (GUESS) / 自信なし (LOW) / まあまあ (MEDIUM) / 自信あり (HIGH), wrapping on narrow screens.
   - Default: **not selected** (`null`), per LEARNING_LOOP.md. Not selected is treated as uncertain:
     review comes early like LOW, and a correct answer earns 0.75 mastery credit (LOW 0.5, MEDIUM/HIGH 1.0).
5. **Footer**:
   - Primary Action: "Submit Answer" (Enabled when choice selected).

---

## 3. Explanation Screen (`ExplanationScreen`)

### Layout Components:
1. **Result Banner**:
   - Prominent Correct (Green) or Incorrect (Red) status card.
2. **Correct Choice Summary**:
   - Highlighted correct answer and key reason.
3. **Distractor Analysis Accordion**:
   - Expandable breakdown for each of the 4 choices explaining why it is correct or incorrect.
4. **Mistake Reason Selector** (Visible if incorrect):
   - Chip group for selecting reason tag (Concept, Terminology, Misread, Careless).
5. **Footer**:
   - Primary Action: "Next Question" or "View Results" (on last question).

---

## 4. Result Screen (`ResultScreen`)

### Layout Components:
1. **Score Summary Card**:
   - Accuracy percentage and correct count.
2. **Reflection Card** (ふりかえり):
   - Total time taken.
   - Confidence × correctness as text rows (e.g. 「自信あり：正解 3 ／ 不正解 0」), plus a note when correct
     answers were given with GUESS / LOW confidence.
   - LOs answered correctly and confidently in this session vs LOs needing review (any wrong, GUESS or LOW answer).
3. **Recommended Next Action CTA**:
   - Prominent button that starts the next session directly: weakness session when accuracy < 80% (daily mix if
     there is no weak question), otherwise the daily mix.
