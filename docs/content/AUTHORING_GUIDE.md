# Question Authoring Guide: TestReason

All questions target the **JSTQB® Foundation Level シラバス 2023V4.0.J02**.
Policy background: [ADR-0006](../adr/ADR-0006-syllabus-content-policy.md).

---

## 1. Non-negotiable rules

1. **Do not copy syllabus sentences.** Write stems, scenarios, choices and explanations in your own
   words with your own examples. Citing a section number is fine; quoting a paragraph is not.
2. **Do not copy or adapt** official sample exams, past exams, or other question banks.
3. **Use the syllabus terminology** (2023V4.0.J02): 欠陥 / 故障 / エラー / 根本原因, 開発担当者,
   メトリクス, 受け入れテスト, テストベース, テストウェア, テストオラクル, etc.
   Do not write 「バグ」「不具合」「受入テスト」「開発者」「メトリック」.
4. **Every choice has an analysis** explaining why it is correct or why it is wrong.
5. **One LO per question.** The LO must be the one the question actually tests.

## 2. Matching the K-level

| K | What the question asks | Typical stem |
|---|---|---|
| K1 (記憶) | Recognise/recall a term, fact or list item | 「〜に該当するものはどれか」 |
| K2 (理解) | Distinguish, compare, classify, give an example, explain why | 「〜の例として最も適切なものはどれか」「AとBの違いを正しく説明しているものはどれか」 |
| K3 (適用) | Apply a technique to a given situation and derive a result | 「次の仕様に境界値分析(2値)を適用した場合、必要なテストケースの最小数はどれか」 |

K3 questions **must** include a `scenario` (specification, table, state model, user story, numbers)
and the explanation **must** show the derivation step by step. Every K3 answer must be recomputed
by a reviewer.

## 3. Question format

- `selectCount: 1` → exactly **4** choices `A`–`D`, exactly 1 correct.
- `selectCount: 2` (「2つ選べ」) → exactly **5** choices `A`–`E`, exactly 2 correct. Use for about
  10% of questions, mainly K1/K2 list-type knowledge.
- Spread the correct answer position roughly evenly across A–D.
- Avoid 「すべて正しい」「いずれも誤り」 choices.
- Negative stems (「適切でないもの」) are allowed sparingly; write the negative word in 「」 so it is visible.
- Choices should be similar in length and plausibility; a distractor should reflect a real misconception.

## 4. File format

One or more files per chapter: `core/src/main/assets/questions/ch{N}.json` or `ch{N}_{part}.json`.
Every `*.json` file in that folder is loaded.

```json
{
  "chapter": 4,
  "questions": [
    {
      "id": "c4-001",
      "lo": "FL-4.2.2",
      "revision": 1,
      "selectCount": 1,
      "scenario": "会員ランクは購入金額(円, 整数)で決まる。\n| 購入金額 | ランク |\n|---|---|\n| 0〜9,999 | 一般 |\n| 10,000〜49,999 | シルバー |\n| 50,000以上 | ゴールド |",
      "stem": "2値境界値分析を適用した場合、境界値として正しい組み合わせはどれか。",
      "choices": [
        { "id": "A", "text": "…", "correct": false, "analysis": "…" },
        { "id": "B", "text": "…", "correct": true,  "analysis": "…" },
        { "id": "C", "text": "…", "correct": false, "analysis": "…" },
        { "id": "D", "text": "…", "correct": false, "analysis": "…" }
      ],
      "explanation": "…（導出手順。必要ならシラバス節番号 4.2.2 を参照と書く）"
    }
  ]
}
```

| Field | Rule |
|---|---|
| `id` | `c{chapter}-{3-digit serial}` (when a chapter is split into parts, each part uses its own hundred block, e.g. `c4-001`.., `c4-101`..). **Never reuse or renumber** an id; it links to the user's history. |
| `lo` | `FL-x.y.z`, must exist in `core/.../model/Syllabus.kt`. |
| `revision` | Start at 1. Increment whenever the text/answer changes, so installed apps pick up the fix. |
| `selectCount` | 1 or 2 (see §3). |
| `scenario` | `null` or text. `\n` for line breaks. Markdown-style tables (`| a | b |` rows, `|---|` separator) are rendered as tables. **The first row must be a header row followed by the separator**: it is shown in bold and read to screen-reader users as the column names of every other row. |
| `stem`, `text`, `analysis`, `explanation` | Non-empty, Japanese. |
| `active` | Optional, default `true`. Set `false` to retire a question without deleting it. |

## 5. Quantity targets

- K1 / K2 LO: at least **3** questions.
- K3 LO: at least **6** questions with different scenarios or numbers.

`QuestionContentValidationTest` fails on schema errors and prints the per-LO coverage report.

## 6. Review checklist (record the result in the PR)

- [ ] No copied syllabus/sample-exam sentences
- [ ] LO and K-level match what is actually asked
- [ ] Exactly the required number of correct choices; the answer is unambiguous
- [ ] K3: answer recomputed independently
- [ ] Terminology follows 2023V4.0.J02
- [ ] Analysis for every choice

## 7. Glossary (`core/src/main/assets/glossary/terms.json`)

One entry per keyword listed at the top of a syllabus chapter (keywords are examinable at K1).

```json
{ "id": "g-001", "term": "カバレッジ", "chapters": [1, 4], "summary": "…。" }
```

- `term` is the keyword exactly as the syllabus spells it. A keyword listed in two chapters is **one** entry with both chapters.
- `summary` is **one line in our own words**, at most 60 characters, ending with 「。」. Do not copy the syllabus or
  the ISTQB/JSTQB glossary definition; say what distinguishes the term from the ones it is confused with
  (エラー / 欠陥 / 故障, 検証 / 妥当性確認, …).
- Use syllabus terminology (section 1, rule 3).
- `GlossaryContentValidationTest` checks the per-chapter keyword counts, the format, and reports keywords that no
  question mentions.
- A reviewer who is not the author checks every summary against the syllabus and the keyword lists per chapter.
