package jp.co.testreason.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.feature.analytics.AnalyticsContent
import jp.co.testreason.feature.review.ReviewContent
import jp.co.testreason.feature.review.ReviewUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Review and analytics screens on every [TestDevice]. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1400dp-h1400dp")
// Real text measurement: in LEGACY mode text has almost no width, which would hide overflow.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProgressScreensResponsiveTest(private val device: TestDevice) {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun reviewWithDueQuestions() {
        val host = compose.setResponsiveContent(device) {
            ReviewContent(
                uiState = ReviewUiState(isLoading = false, dueQuestions = List(3) { ScreenFixtures.scenarioQuestion.copy(id = "q$it") }),
                onStartReview = {}
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("復習を開始（3問）").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reviewEmpty() {
        compose.setResponsiveContent(device) {
            ReviewContent(uiState = ReviewUiState(isLoading = false), onStartReview = {})
        }
        compose.assertNoHorizontalOverflow()
        compose.onNodeWithText("復習対象はありません").assertIsDisplayed()
    }

    @Test
    fun analytics() {
        val host = compose.setResponsiveContent(device) {
            AnalyticsContent(uiState = ScreenFixtures.dashboard)
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("K3（適用）", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("用語の混同：1回").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("弱点 50%").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("第6章 テストツール").performScrollTo().assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun devices() = TestDevice.parameters()
    }
}
