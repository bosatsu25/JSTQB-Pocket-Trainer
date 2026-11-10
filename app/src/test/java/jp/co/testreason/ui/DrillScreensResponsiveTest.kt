package jp.co.testreason.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.feature.drill.DrillActions
import jp.co.testreason.feature.drill.DrillContent
import jp.co.testreason.feature.drill.DrillLap
import jp.co.testreason.feature.drill.DrillReveal
import jp.co.testreason.feature.drill.DrillSetupContent
import jp.co.testreason.feature.drill.DrillSetupUiState
import jp.co.testreason.feature.drill.DrillUiState
import jp.co.testreason.feature.drill.drillChapters
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Rapid drill (周回モード) screens on every [TestDevice]. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1400dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DrillScreensResponsiveTest(private val device: TestDevice) {

    @get:Rule
    val compose = createComposeRule()

    private val question = ScreenFixtures.scenarioQuestion
    private val lap = DrillLap.start(listOf(question.id, "c1-001", "c1-002"))

    @Test
    fun setup() {
        val host = compose.setResponsiveContent(device) {
            DrillSetupContent(
                uiState = DrillSetupUiState(drillChapters(emptyList()).map { it.copy(questionCount = 63) }),
                onStartDrill = { _, _ -> }
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("習熟度・復習予定・弱点には反映しません", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("全範囲から").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("40問").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("第6章 テストツール").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("全63問").onLast().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun question_beforeAnswering() {
        val host = compose.setResponsiveContent(device) {
            DrillContent(uiState = DrillUiState(isLoading = false, lap = lap, question = question), actions = DrillActions())
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("周回モード 0 / 3 クリア").assertIsDisplayed()
        compose.onNodeWithText("分からないので答えを見る").performScrollTo().assertIsDisplayed()

        // positionInRoot is not clipped by scrolling, unlike boundsInRoot.
        val label = compose.onNodeWithText("FL-4.2.2").fetchSemanticsNode()
        val choices = compose.onNodeWithText("2つ選択").fetchSemanticsNode()
        if (device.isWide) {
            assertTrue("choices should be in a right-hand pane", choices.positionInRoot.x >= label.positionInRoot.x + label.size.width)
        } else {
            assertTrue("choices should be below the question", choices.positionInRoot.y >= label.positionInRoot.y + label.size.height)
        }
    }

    @Test
    fun question_answerRevealedWithReasons() {
        val host = compose.setResponsiveContent(device) {
            DrillContent(
                uiState = DrillUiState(
                    isLoading = false,
                    lap = lap,
                    question = question,
                    selectedChoiceIds = setOf("A", "B"),
                    reveal = DrillReveal.WRONG,
                    showReasons = true
                ),
                actions = DrillActions()
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        // The verdict is text, not only colour.
        compose.onNodeWithText("正解（あなたの解答）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("あなたの解答（不正解）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("解説D").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("不正解").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("次へ").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("各選択肢の理由を閉じる").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun nextQuestion_startsAtTheTop() {
        var state by mutableStateOf(
            DrillUiState(isLoading = false, lap = lap, question = question, selectedChoiceIds = setOf("B", "D"), reveal = DrillReveal.CORRECT)
        )
        compose.setResponsiveContent(device) { DrillContent(uiState = state, actions = DrillActions()) }
        compose.onNodeWithText("次へ").performScrollTo().assertIsDisplayed()

        state = DrillUiState(isLoading = false, lap = lap.answered(correct = true), question = question.copy(id = "c1-001", learningObjectiveId = "FL-1.1.1"))
        compose.waitForIdle()

        compose.onNodeWithText("FL-1.1.1").assertIsDisplayed()
    }

    @Test
    fun lapSummary() {
        val finished = lap.answered(correct = false).answered(correct = true).answered(correct = true).answered(correct = true)
        val host = compose.setResponsiveContent(device) {
            DrillContent(uiState = DrillUiState(isLoading = false, lap = finished, elapsedMs = 95_000L), actions = DrillActions())
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("3問中、1回目で正解 2問").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("解答回数 4回 ・ 1分35秒").performScrollTo().assertIsDisplayed()
        listOf("間違えた1問だけもう1周", "同じ問題でもう1周", "章別学習で理由を確認", "ホームへ戻る").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun emptyScope() {
        val host = compose.setResponsiveContent(device) {
            DrillContent(uiState = DrillUiState(isLoading = false), actions = DrillActions())
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onAllNodesWithText("この範囲の問題はまだありません。").onFirst().assertIsDisplayed()
        compose.onNodeWithText("ホームへ戻る").performScrollTo().assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun devices() = TestDevice.parameters()
    }
}
