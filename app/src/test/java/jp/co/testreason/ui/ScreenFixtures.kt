package jp.co.testreason.ui

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.QuizSession
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.StudyMode
import jp.co.testreason.feature.analytics.buildDashboard
import jp.co.testreason.feature.result.ConfidenceRow
import jp.co.testreason.feature.result.ObjectiveResult
import jp.co.testreason.feature.result.SessionSummary

/** Worst-case content for layout tests: long text, a wide table and a 5-choice multi-answer question. */
object ScreenFixtures {

    private const val LONG_TEXT = "ある会員制ECサイトでは、購入金額と会員ランクに応じて割引率とポイント付与率が決まり、" +
        "さらにキャンペーン期間中は特定のカテゴリーの商品に追加のクーポンが適用される。"

    val scenarioQuestion = Question(
        id = "c4-001",
        learningObjectiveId = "FL-4.2.2",
        stem = "$LONG_TEXT この仕様に2値境界値分析を適用したとき、境界値として正しい組み合わせはどれか。",
        scenario = "会員ランクは購入金額（円、整数）で決まる。\n" +
            "| 購入金額 | ランク | 割引率 | ポイント付与率 | 備考 |\n|---|---|---|---|---|\n" +
            "| 0〜9,999 | 一般 | 0% | 1% | なし |\n| 10,000〜49,999 | シルバー | 3% | 2% | 誕生月は+1% |\n" +
            "| 50,000以上 | ゴールド | 5% | 3% | 送料無料 |\n" +
            "```\nif (amount >= 50000) rank = GOLD else if (amount >= 10000) rank = SILVER else rank = NORMAL\n```",
        choices = listOf("A", "B", "C", "D", "E").map { id ->
            Choice(id, "選択肢$id：9,999 と 10,000、49,999 と 50,000 を含む境界値の組み合わせ（$LONG_TEXT）", id in setOf("B", "D"), "解説$id")
        },
        explanation = LONG_TEXT.repeat(3),
        selectCount = 2
    )

    val session = QuizSession("s1", StudyMode.DAILY, List(5) { "q$it" }, 2, 0L, null, false)

    val wrongAttempt = Attempt("a1", "s1", scenarioQuestion.id, listOf("A", "C"), false, ConfidenceLevel.LOW, null, 1_000L, 0L)

    val summary = SessionSummary(
        totalTimeMs = 185_000L,
        confidenceRows = listOf(
            ConfidenceRow(null, 1, 0),
            ConfidenceRow(ConfidenceLevel.GUESS, 1, 1),
            ConfidenceRow(ConfidenceLevel.HIGH, 0, 2)
        ),
        strongObjectives = listOf(ObjectiveResult("FL-1.1.1", "テストの典型的な目的")),
        reviewObjectives = listOf(
            ObjectiveResult("FL-5.1.2", "イテレーション計画・リリース計画へのテスト担当者の貢献"),
            ObjectiveResult("FL-4.2.4", "状態遷移テストの適用")
        )
    )

    /** Dashboard with every section filled: a weak K3 LO, tagged and untagged misses. */
    val dashboard = buildDashboard(
        questions = listOf(scenarioQuestion, scenarioQuestion.copy(id = "c1-001", learningObjectiveId = "FL-1.1.1")),
        attempts = listOf(
            wrongAttempt.copy(mistakeReason = MistakeReason.TERMINOLOGY),
            wrongAttempt.copy(id = "a2", sessionId = "s2"),
            wrongAttempt.copy(id = "a3", sessionId = "s3", questionId = "c1-001", isCorrect = true)
        ),
        masteries = listOf(Mastery("FL-4.2.2", 0.5f, 0L, 2, 1), Mastery("FL-1.1.1", 1f, 0L, 3, 3))
    )
}

