# ADR-0006: Syllabus Content & Trademark Policy

## Context
TestReason maps every question to a Learning Objective (LO) of the
**JSTQB Foundation Level シラバス Version 2023V4.0.J02**.

- The ISTQB® copyright notice allows derivative works (articles, books, training material)
  *as long as the authors and ISTQB® are acknowledged as the source and copyright owners*.
- The Japanese translation is separately copyrighted by JSTQB®, which states that copying or
  using all or part of the translated text without permission is prohibited except as allowed
  by copyright law (syllabus p.3).
- "ISTQB" and "JSTQB" are registered trademarks.

If the content policy changes after questions are written, every question would have to be
rewritten. The policy must therefore be fixed before content authoring starts (Issue #3).

## Decision
We take the conservative option that does not depend on any permission from JSTQB®.

1. **No verbatim syllabus text in the app.**
   - LOs are displayed as `LO ID + K-level + an original short summary` written by us
     (e.g. `FL-4.2.2 (K3) 境界値分析でテストケースを導く`). The official LO sentence is not copied.
   - Explanations do not quote syllabus paragraphs. They explain in our own words and cite the
     section number only (`syllabusRef = "4.2.2"`).
2. **Original questions only.**
   - Question stems, scenarios, choices and explanations are written from scratch with our own
     examples.
   - Official sample exams, past exam questions and third-party question banks must not be
     copied or adapted.
3. **Terminology follows the syllabus.** Technical terms (欠陥, 故障, 開発担当者, メトリクス, …)
   follow the 2023V4.0.J02 wording. Single terms are not protected expression; consistent
   terminology is required for exam preparation.
4. **Trademarks.**
   - The product name stays **TestReason**. "JSTQB" / "ISTQB" are not part of the app name or icon.
   - "JSTQB®" may appear only descriptively (e.g. "JSTQB® Foundation Level シラバス 2023V4.0.J02 の
     学習目的に対応").
5. **Disclaimer and attribution** are shown in the app ("このアプリについて") and store listing:
   - This app is unofficial and is not affiliated with, endorsed by, or certified by ISTQB® or JSTQB®.
   - ISTQB® and JSTQB® are registered trademarks of their respective owners.
   - LO IDs and K-levels refer to the JSTQB® Foundation Level syllabus Version 2023V4.0.J02
     (© ISTQB®, Japanese translation © JSTQB®).

## Status
Accepted (owner approved proceeding with the recommended option in Issue #3).
Re-open if the owner decides to request permission from JSTQB® to reproduce LO text.

## Consequences
### Positive
- No dependency on permission from JSTQB® to ship.
- Content can be reviewed against a single, simple rule: "Is any sentence copied?"

### Negative
- LO summaries must be written and reviewed by hand (64 items).
- Users must read the official syllabus for the exact LO wording.
