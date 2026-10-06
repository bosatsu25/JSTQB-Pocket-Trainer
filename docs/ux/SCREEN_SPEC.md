# Screen Specifications: TestReason

---

## 1. Home Screen (`HomeScreen`)

### Layout Components:
1. **Top Bar**: App Title ("TestReason"), Current Overall Mastery Badge.
2. **Hero Card**: "Today's Recommended Plan"
   - Primary Action Button: "Start Daily Quiz (5 Qs)"
   - Sub-caption: Estimated time ~3 mins.
3. **Quick Action Grid**:
   - Chapter Study
   - Weakness Review
   - Mock Exam (40 Qs)
4. **Recent Activity Summary**:
   - Last session score & LO progress trend.

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
4. **Confidence Selector**:
   - 4 segmented buttons (Guess / Low / Medium / High). Default: Medium.
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
   - Accuracy percentage, total time taken, confidence distribution chart.
2. **Mastery Delta Breakdown**:
   - LOs improved vs LOs needing review.
3. **Recommended Next Action CTA**:
   - Prominent button linking directly to the next best learning session.
