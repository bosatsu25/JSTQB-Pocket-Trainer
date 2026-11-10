package jp.co.testreason.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.core.model.MockExam
import jp.co.testreason.core.model.MockExamQuestion
import jp.co.testreason.core.model.MockExamResult
import jp.co.testreason.core.model.MockExamResultItem
import jp.co.testreason.core.model.ScoreLine
import jp.co.testreason.feature.mockexam.MockExamActions
import jp.co.testreason.feature.mockexam.MockExamContent
import jp.co.testreason.feature.mockexam.MockExamIntroContent
import jp.co.testreason.feature.mockexam.MockExamIntroUiState
import jp.co.testreason.feature.mockexam.MockExamResultContent
import jp.co.testreason.feature.mockexam.MockExamResultUiState
import jp.co.testreason.feature.mockexam.MockExamUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Mock exam screens on every [TestDevice], including a 40-cell navigator and the submit dialog. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1400dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MockExamResponsiveTest(private val device: TestDevice) {

    @get:Rule
    val compose = createComposeRule()

    private val questions = List(40) { i ->
        MockExamQuestion(
            question = ScreenFixtures.scenarioQuestion.copy(id = "q$i"),
            selectedChoiceIds = if (i % 3 == 0) setOf("B", "D") else emptySet(),
            flagged = i % 7 == 0
        )
    }
    private val exam = MockExam("s1", questions, startedAt = 0L, durationMinutes = 60, submittedAt = null)

    @Test
    fun intro() {
        val host = compose.setResponsiveContent(device) {
            MockExamIntroContent(MockExamIntroUiState(), onExtendedTimeChange = {}, onStart = {}, onResume = {})
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("時間延長（75分）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("模擬試験を開始").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("途中の模擬試験を再開").assertDoesNotExist()
    }

    @Test
    fun introWithAnOpenExam_offersOnlyResume() {
        val host = compose.setResponsiveContent(device) {
            MockExamIntroContent(MockExamIntroUiState(resumableExamId = "s0"), onExtendedTimeChange = {}, onStart = {}, onResume = {})
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("途中の模擬試験を再開").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("模擬試験を開始").assertDoesNotExist()
    }

    @Test
    fun exam() {
        val host = compose.setResponsiveContent(device) {
            MockExamContent(MockExamUiState(exam = exam, currentIndex = 0, remainingMs = 4 * 60_000L), MockExamActions())
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("残り 4:00（残りわずか）").assertIsDisplayed()
        compose.onNodeWithText("見直し解除").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("問題38 未回答").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("提出する").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun submitDialog() {
        compose.setResponsiveContent(device) {
            MockExamContent(MockExamUiState(exam = exam, remainingMs = 600_000L, isConfirmingSubmit = true), MockExamActions())
        }
        compose.onNodeWithText("未回答が 26 問あります。未回答は不正解として採点します。").assertIsDisplayed()
        compose.onNodeWithText("戻る").assertIsDisplayed()
    }

    @Test
    fun result() {
        val items = questions.mapIndexed { i, q -> MockExamResultItem(i + 1, q.question, q.selectedChoiceIds, i % 2 == 0) }
        val result = MockExamResult(
            sessionId = "s1",
            correct = 25,
            total = 40,
            byChapter = listOf(ScoreLine("第5章 テスト活動のマネジメント", 5, 9), ScoreLine("第4章 テスト分析と設計", 7, 11)),
            byKLevel = listOf(ScoreLine("K1", 6, 8), ScoreLine("K2", 15, 24), ScoreLine("K3", 4, 8)),
            items = items
        )
        val host = compose.setResponsiveContent(device) {
            MockExamResultContent(MockExamResultUiState(result = result), onStartWeakness = {}, onNavigateHome = {})
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("合格ライン未達").assertIsDisplayed()
        compose.onNodeWithText("間違えた問題を弱点克服で復習").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("問40", substring = true).performScrollTo().assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun devices() = TestDevice.parameters()
    }
}
