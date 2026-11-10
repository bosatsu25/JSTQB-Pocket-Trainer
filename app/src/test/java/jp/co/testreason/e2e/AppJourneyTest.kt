package jp.co.testreason.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end journeys through the real app on the JVM: [MainActivity] with the real Hilt graph, the real
 * Room database and the bundled content. Checks what the per-screen tests cannot: that the dependency
 * graph starts, that the screens are wired together, and that navigation and the back stack behave.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppJourneyTest {

    @get:Rule(order = 0)
    val drainMainLooper = DrainMainLooperRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val app = AppRobot(compose)

    private fun back() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun dailySession_fromHomeToResultAndBackHome() {
        app.tap("今日のおすすめを開始")
        repeat(DAILY_QUESTIONS) { index ->
            app.answerQuizQuestion(index + 1, DAILY_QUESTIONS)
            app.tap(if (index == DAILY_QUESTIONS - 1) "結果を見る" else "次の問題へ")
        }

        app.waitFor("セッション結果")
        app.waitFor("/ $DAILY_QUESTIONS 問正解", substring = true)
        app.tap("ホームへ戻る")

        app.waitFor("今日のおすすめを開始")
        compose.onNodeWithText("TestReason").assertIsDisplayed()
        // A finished session is not offered for resuming.
        compose.onNodeWithText("続きから再開").assertDoesNotExist()
        back() // Home is the only entry left on the back stack
    }

    @Test
    fun backFromTheExplanation_keepsTheSession_andResumeContinuesIt() {
        app.tap("今日のおすすめを開始")
        app.answerQuizQuestion(1, DAILY_QUESTIONS)

        back() // the quiz screen was replaced by the explanation, so back leads Home

        app.tap("続きから再開") // also proves that Home unlocked its buttons when it was shown again
        app.answerQuizQuestion(1, DAILY_QUESTIONS) // the unfinished question again; the first answer is kept
        app.tap("次の問題へ")
        app.answerQuizQuestion(2, DAILY_QUESTIONS)
    }

    @Test
    fun mockExam_canBeLeftAndResumed_thenSubmittedWithUnansweredQuestions() {
        app.tap("模擬試験")
        app.tap("模擬試験を開始")
        app.waitFor("問題 1 / 40")
        app.waitFor("残り ", substring = true)
        app.selectChoices()
        app.waitFor("回答済み 1 / 40", substring = true)

        back()
        app.tap("途中の模擬試験を再開")
        app.waitFor("回答済み 1 / 40", substring = true)

        app.tap("提出する")
        app.waitFor("提出しますか？")
        compose.onAllNodesWithText("提出する").onLast().performClick()

        app.waitFor("模擬試験の結果")
        app.waitFor("未回答 39 問")
        app.tap("ホームへ戻る")
        app.waitFor("今日のおすすめを開始")

        // The submitted exam is closed: a new one can be started.
        app.tap("模擬試験")
        app.waitFor("模擬試験を開始")
        compose.onNodeWithText("途中の模擬試験を再開").assertDoesNotExist()
    }

    @Test
    fun rapidDrill_showsTheAnswerOnTheTap_andLeadsBackHome() {
        app.tap("周回モード")
        app.waitFor("全範囲から")
        compose.onAllNodesWithText("20問").onFirst().performScrollTo().performClick()
        app.waitFor("周回モード 0 / 20 クリア")

        app.tap("分からないので答えを見る")
        app.waitFor("答えを確認")
        app.tap("なぜ？（各選択肢の理由を見る）")
        app.waitFor("各選択肢の理由を閉じる")
        app.tap("次へ")

        app.waitOutTheTapGuard()
        app.selectChoices() // judged on the tap
        app.waitFor("次へ")

        back()
        app.waitFor("全範囲から")
        back()
        app.waitFor("今日のおすすめを開始")
    }

    @Test
    fun glossary_filtersByChapter_andCardsLeadBackToTheList() {
        app.tap("用語カード")
        app.waitFor("用語ひとこと")
        app.waitFor("全章（94）")
        app.tap("第6章（1）")
        app.tap("カードで確認する（1語）")

        app.waitFor("用語カード 0 / 1")
        compose.onNodeWithText("テスト自動化").assertIsDisplayed()
        app.tap("意味を見る")
        app.waitOutTheTapGuard()
        app.tap("覚えた")

        app.waitFor("1周クリア")
        app.tap("用語一覧へ戻る")
        app.waitFor("用語ひとこと")
        back()
        app.waitFor("今日のおすすめを開始")
    }

    @Test
    fun learningMenu_opensReviewChaptersAndAnalytics() {
        app.tap("今日の復習")
        app.waitFor("復習対象はありません")
        back()

        app.tap("学習分析")
        app.waitFor("知識レベル別の正答率")
        back()

        app.tap("章別学習")
        app.waitFor("第1章 テストの基礎")
        compose.onAllNodesWithText("5問").onFirst().performScrollTo().performClick()
        app.waitFor("問題 1 / 5")
        back()
        app.waitFor("第1章 テストの基礎") // chapter select stays on the back stack
        back()
        app.waitFor("続きから再開")
    }

    private companion object {
        const val DAILY_QUESTIONS = 5
    }
}
