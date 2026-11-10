# Exam Format: JSTQB Foundation Level (syllabus 2023V4.0.J02)

Researched 2026-11-10 for the mock exam (issue #19). Re-check the sources when ISTQB publishes a new
"Exam Structure and Rules Tables" version or JSTQB changes the exam guide.

## Sources
| Source | What it states |
|---|---|
| JSTQB exam guide — https://www.jstqb.jp/guidance | Foundation Level: multiple choice, **40 questions, 60 minutes**. JSTQB does not publish the pass line and refers to the ISTQB site. |
| ISTQB Exam Structure and Rules Tables v1.18 (May 27, 2026) — https://istqb.org/wp-content/uploads/2026/05/ISTQB_Exam-Structure-and-Rules-Tables_v1.18.pdf | CTFL v4.0: 40 questions, 40 points, **pass at 26 points (65%)**, 60 minutes, 75 minutes with +25%. K-level, chapter and LO-group distribution below. |
| Pearson VUE (JSTQB page) — https://www.pearsonvue.com/jp/ja/jstqb.html | Total sitting 75 minutes = NDA 5 + exam 60 + survey 10. Non-native Japanese speakers may get 1.25× time with prior approval. |
| JSTQB syllabus page — https://www.jstqb.jp/syllabus | The FL syllabus currently published is byte-identical to Version 2023V4.0.J02 (checked 2026-11-10). |

## Exam rules used by the app
- 40 questions, 1 point each, single best answer per question (some questions ask for two answers; graded as one item).
- 60 minutes (optional 75-minute mode for non-native speakers).
- Pass: **≥ 26 correct (65%)**. 25 correct is a fail.

## Distribution (ISTQB v1.18)
| K-level | Questions |
|---|---|
| K1 | 8 |
| K2 | 24 |
| K3 | 8 |

| Chapter | Questions | K1 | K2 | K3 |
|---|---|---|---|---|
| 1 | 8 | 2 | 6 | 0 |
| 2 | 6 | 2 | 4 | 0 |
| 3 | 4 | 2 | 2 | 0 |
| 4 | 11 | 0 | 6 | 5 |
| 5 | 9 | 1 | 5 | 3 |
| 6 | 2 | 1 | 1 | 0 |

LO groups (each group contributes the stated number of questions, drawn from any of its LOs):

| Group (LOs) | K | Questions |
|---|---|---|
| 1.1.1, 1.2.2 | K1 | 1 |
| 1.5.2 | K1 | 1 |
| 1.1.2, 1.2.1 | K2 | 1 |
| 1.2.3, 1.3.1 | K2 | 1 |
| 1.4.1, 1.4.2, 1.4.3, 1.4.4, 1.4.5 | K2 | 3 |
| 1.5.1, 1.5.3 | K2 | 1 |
| 2.1.2 | K1 | 1 |
| 2.1.3 | K1 | 1 |
| 2.2.1, 2.2.2 | K2 | 1 |
| 2.2.3, 2.3.1 | K2 | 1 |
| 2.1.1, 2.1.6 | K2 | 1 |
| 2.1.4, 2.1.5 | K2 | 1 |
| 3.1.1, 3.2.1, 3.2.3, 3.2.5 | K1 | 2 |
| 3.1.2, 3.1.3 | K2 | 1 |
| 3.2.2, 3.2.4 | K2 | 1 |
| 4.1.1 | K2 | 1 |
| 4.3.1, 4.3.2, 4.3.3 | K2 | 2 |
| 4.4.1, 4.4.2, 4.4.3 | K2 | 2 |
| 4.5.1, 4.5.2 | K2 | 1 |
| 4.2.1, 4.2.2, 4.2.3, 4.2.4, 4.5.3 | K3 | 5 |
| 5.1.2, 5.1.6, 5.2.1, 5.3.1 | K1 | 1 |
| 5.1.1, 5.1.3 | K2 | 1 |
| 5.1.7 | K2 | 1 |
| 5.2.2, 5.2.3, 5.2.4 | K2 | 1 |
| 5.3.2, 5.3.3 | K2 | 1 |
| 5.4.1 | K2 | 1 |
| 5.1.4, 5.1.5, 5.5.1 | K3 | 3 |
| 6.1.1 | K2 | 1 |
| 6.2.1 | K1 | 1 |

Note on reading the PDF: the 5.3.2/5.3.3 group is split across a page break and its row is printed on both pages;
the chapter totals (K2 = 5 for chapter 5) confirm it is a single group of one question.

The table is encoded in `core/.../model/ExamBlueprint.kt` and pinned by `ExamBlueprintTest`.
