# Screen Specifications: TestReason

---

## 1. Home Screen (`HomeScreen`)

### Layout Components:
1. **Top Bar**: App Title ("TestReason").
2. **Mastery Card**: overall mastery (average of LO masteries).
3. **Resume Card** (only while an unfinished session has questions left): progress and "続きから再開".
4. **Hero Card**: "今日のおすすめ学習" — due → weak → unseen questions. The size is the daily goal from the
   settings (5 / 10 / 20 questions, shown with the estimated minutes; default 5 ≈ 3 minutes).
5. **Learning Menu**:
   - 今日の復習 (due count) → Review screen
   - 弱点克服 (weak count) → starts a weakness session, or a message when there is none
   - 章別学習 → Chapter Select screen
   - 周回モード → Rapid Drill setup (§8)
   - 用語カード → Glossary (§9)
   - 学習分析 → learning analytics
   - 模擬試験 → Mock Exam intro
   - 設定 → Settings (§10)
   - The menu wraps (FlowRow) on narrow screens or large fonts.

---

## 1b. Chapter Select Screen (`ChapterSelectScreen`)
- One card per syllabus chapter: title, question count, unseen count, mastery (or 未着手).
- Count buttons: 5問 / 10問 (when the chapter has more) / 全N問. Questions are ordered unseen → weak
  (oldest first) → others (least recently answered first).

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
6. **State restoration**: the selected choices and the confidence survive process death (`SavedStateHandle`),
   for the question they were made on.

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

---

## 5. Analytics Screen (`AnalyticsScreen`)
1. **Accuracy by knowledge level**: K1 / K2 / K3 with correct / answered (or 未回答).
2. **Mistake reasons**: count per tag, plus untagged misses (shown only when there are misses).
3. **Chapter cards**: mastery bar (with text), answered LOs / total, and every LO as a ≥ 48dp row with its
   state in text (未着手 / 弱点 / 学習中 / 習熟) and score. Tapping a row with questions starts an LO session.

---

## 6. Mock Exam (`MockExamIntroScreen` → `MockExamScreen` → `MockExamResultScreen`)
Format and sources: `docs/product/EXAM_FORMAT.md` (ISTQB Exam Structure and Rules Tables v1.18, JSTQB exam guide).

1. **Intro**: rules (40 questions, 60 minutes, pass at 26 = 65%, official K/chapter distribution, no explanations
   until submission), optional 75-minute extended time, start button.
   **Only one exam can be open.** While an exam is unsubmitted the intro shows a note and a resume button instead
   of the extended-time switch and the start button; a new exam can start after that one is submitted (resuming
   after the deadline submits it at once). The repository enforces the same rule.
2. **Exam**: remaining time in the top bar (with a text warning under 5 minutes), question and choices (radio /
   checkbox), review flag, previous / next, a 40-cell navigator whose cells state answered (✓) and flagged (?) in
   text and in their content description, and a submit button with a confirmation dialog that states the number of
   unanswered questions. Two panes from 720dp (question | navigator).
   Time up submits automatically; reopening after the deadline submits immediately.
   Each tap on a choice is applied to the saved answer (not to what the screen shows), so two quick taps on a
   two-answer question keep both choices.
3. **Result**: pass-line reached / not reached, score vs pass line, unanswered count, scores by chapter and by
   K-level, a CTA to start a weakness session with the wrong answers, and every question with its verdict;
   tapping a row shows the stem, the correct vs given answer and the explanation.

---

## 7. Starting a Session (all screens)
A button that starts a session (Home, result, chapter select, analytics, review, mock exam intro / result) is locked
from the tap until the screen is shown again: a second tap must not create a second session or navigate twice.
Screens that stay on the back stack unlock in `onScreenShown()`; screens removed from the back stack stay locked.
If nothing could be started (empty queue or chapter), the button unlocks immediately.


---

## 8. Rapid Drill (`DrillSetupScreen` → `DrillScreen`) — 周回モード (#32)
For getting through many questions quickly: see the answer at once, repeat until every question is answered
correctly, and only then dig into the reasons. Behaviour rules are in `LEARNING_LOOP.md` ("Rapid Drill").

### Setup
- A card explains the mode and states that drill answers do **not** change mastery, review schedule or weak questions.
- Scope buttons: 全範囲から (20問 / 40問), and per chapter (10問 / 20問 / 全N問). Counts not smaller than the
  scope are hidden. An empty chapter shows a note and no button.

### Drill
1. **Top Bar**: "周回モード x / y クリア" (questions answered correctly so far in this lap).
2. **Question card**: LO id, scenario (if any), stem. Two panes (question | choices) from 720dp.
3. **Choices**: a single-answer question is judged on the tap; a multi-answer question when the required number
   is chosen (a choice can be deselected before that). No confidence selector and no submit button.
4. **"分からないので答えを見る"**: reveals the answer without answering; counts as a miss.
5. **After the reveal** (same screen, choices locked): verdict text on the choices ("正解" / "あなたの解答（不正解）",
   never colour alone), headline + explanation, primary **"次へ"**, and **"なぜ？（各選択肢の理由を見る）"** which
   shows why each choice is right or wrong. The reasons are closed again on the next question.
6. **Next question**: starts scrolled to the top. Taps on choices or "答えを見る" within 400 ms after "次へ" are
   ignored — a tap is the answer in this mode, so the second tap of a double tap on "次へ" must not answer the
   next question.

### Lap summary
- 1周クリア: first-try correct count, number of answers, elapsed time, seconds per answer.
- Actions: "間違えたN問だけもう1周" (only when something was missed), "同じ問題でもう1周", "章別学習で理由を確認", "ホームへ戻る".

---

## 9. Glossary (`GlossaryScreen` → `GlossaryCardsScreen`) — 用語ひとこと / 用語カード (#4)
The 94 keywords listed at the top of the six syllabus chapters, each with a one-line summary written for this
app (ADR-0006: not the syllabus or glossary wording).

### List
- A note says the summaries are this app's own and that the exact definitions are in the syllabus and glossary.
- Chapter filter chips with counts: 全章 / 第1章 … 第6章. A keyword listed in two chapters appears in both.
- "カードで確認する（N語）" opens the cards for the selected scope (disabled when the scope is empty).
- Rows: term and its one-line summary (one screen-reader node per row).

### Cards
1. **Top Bar**: "用語カード x / y" (terms marked 覚えた in this lap).
2. **Card**: chapter(s), the term, and a prompt to recall the meaning. "意味を見る" turns the card.
3. **After turning**: the summary, then **"まだ"** and **"覚えた"**. A term marked まだ comes back at the end of
   the lap; the lap ends when every term is marked 覚えた. Taps on まだ / 覚えた within 400 ms after turning are
   ignored (they replace "意味を見る" in the same place). The next card starts face down and at the top.
4. **Lap summary**: first-try count; "「まだ」のN語だけもう1周", "同じ語でもう1周", "用語一覧へ戻る", "ホームへ戻る".

Nothing is recorded (same rule as the rapid drill). The lap is kept in `SavedStateHandle`.

---

## 10. Settings (`SettingsScreen`) (#21)
Stored in DataStore (`UserPreferencesRepository`); a change applies at once and survives a restart.

- **テーマ**: システムに合わせる (default) / ライト / ダーク. The app draws nothing until the stored theme has been
  read, so it never starts in the wrong theme and then switches.
- **1日の目標問題数**: 5 (default) / 10 / 20 — the size of the daily session on Home.
- A stored value this version does not know falls back to the default.
- **このアプリについて** opens the about screen (§11).
- **Not implemented**: the study reminder notification. It needs a new dependency (WorkManager) and the
  notification permission; waiting for the owner's decision in #21.

---

## 11. About (`AboutScreen`) (#25, ADR-0006)
Reached from Settings. Shows what ADR-0006 requires the app to show, plus what a learner needs to know about
their data:

- App name and version (read from the installed package).
- **Unofficial**: not affiliated with, endorsed by or certified by ISTQB® / JSTQB®; both are registered trademarks.
- **Syllabus**: LO ids and K-levels refer to the JSTQB® Foundation Level syllabus (version from `Syllabus.VERSION`,
  © ISTQB®, Japanese translation © JSTQB®). Questions, explanations and glossary summaries are this app's own;
  the official syllabus is the reference. The mock exam follows the published exam structure and does not predict
  the real result.
- **Data**: study records and settings stay on the device; the app has no network permission
  (`AppManifestTest` fails if one is ever added). Device backup may include the data, per the device's settings.
- **Open-source licences**: the shipped libraries and the Apache License 2.0 with its URL.
  The full licence text is not bundled yet (open in #25).
