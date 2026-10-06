# Accessibility Specification (a11y): TestReason

---

## 1. Touch Targets
- All clickable Compose components (buttons, radio choices, chips, confidence buttons) MUST maintain a minimum touch target size of **48dp x 48dp**.

---

## 2. TalkBack & Screen Readers
- Every interactive element must provide explicit `contentDescription` or semantic merge (`Modifier.semantics(mergeDescendants = true)`).
- Result banners must announce state clearly (e.g. "Answer Correct" or "Answer Incorrect").
- Questions and choice items must read logically without skipping headers or metadata tags.

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
