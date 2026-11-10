# Accessibility Specification (a11y): TestReason

---

## 1. Touch Targets
- All clickable Compose components (buttons, radio choices, chips, confidence buttons) MUST maintain a minimum touch target size of **48dp x 48dp**.

---

## 2. TalkBack & Screen Readers
- Every interactive element must provide explicit `contentDescription` or semantic merge (`Modifier.semantics(mergeDescendants = true)`).
- Result banners must announce state clearly (e.g. "Answer Correct" or "Answer Incorrect").
- Questions and choice items must read logically without skipping headers or metadata tags.
- **Names and roles (automated)**: every clickable element has a name, and every selectable or toggleable element
  has a role (radio button, checkbox, …). `assertAccessible` checks this on the merged semantics tree for every
  screen on every test device.
- **Scenario tables**: a single cell ("3%") means nothing without its column. Each table row is one node that is
  read as a sentence pairing every value with its column header ("2行目。購入金額 0〜9,999、ランク 一般、…");
  the header row is read as "表の見出し。…". An empty cell is read as 空欄. Tables therefore need a header row
  (enforced by `QuestionContentValidationTest`).
- **Mock exam clock**: the remaining time changes every second and is **not** a live region. The low-time warning
  "（残りわずか）" is a separate, polite live region, so it is announced once when it appears.
- **Not verified on a device**: all of the above is checked at the semantics level only. What TalkBack actually
  says, the reading order, and an Accessibility Scanner pass need a device or emulator (#23).

---

## 3. Font Scaling & Dynamic Type
- Layouts must support font scaling up to **200%** (`sp` units) without clipping text, overlapping elements, or truncating choices.
- Choice containers must scale vertically with text height.

---

## 4. Color & Contrast
- Color contrast ratios must meet WCAG AA standards:
  - Minimum **4.5:1** for normal body text.
  - Minimum **3:1** for large text and UI components/state indicators.
- Never rely solely on color to convey state (e.g., correct/incorrect indicators must include text or icons alongside color changes).
