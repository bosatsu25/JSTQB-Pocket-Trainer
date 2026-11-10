package jp.co.testreason.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.core.datastore.DarkThemeConfig
import jp.co.testreason.core.datastore.UserPreferences
import jp.co.testreason.feature.about.AboutContent
import jp.co.testreason.feature.chapter.ChapterSelectContent
import jp.co.testreason.feature.chapter.ChapterSelectUiState
import jp.co.testreason.feature.chapter.chapterOverview
import jp.co.testreason.feature.explanation.ExplanationContent
import jp.co.testreason.feature.explanation.ExplanationUiState
import jp.co.testreason.feature.home.HomeActions
import jp.co.testreason.feature.home.HomeContent
import jp.co.testreason.feature.home.HomeUiState
import jp.co.testreason.feature.quiz.QuizContent
import jp.co.testreason.feature.quiz.QuizUiState
import jp.co.testreason.feature.result.RecommendedAction
import jp.co.testreason.feature.result.ResultContent
import jp.co.testreason.feature.result.ResultUiState
import jp.co.testreason.feature.settings.SettingsContent
import jp.co.testreason.feature.settings.SettingsUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Learning-loop screens on every [TestDevice]: nothing overflows, touch targets are big enough, actions are reachable. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1400dp-h1400dp")
// Real text measurement: in LEGACY mode text has almost no width, which would hide overflow.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudyScreensResponsiveTest(private val device: TestDevice) {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun home() {
        val host = compose.setResponsiveContent(device) {
            HomeContent(
                uiState = HomeUiState(
                    averageMastery = 0.42f,
                    activeSession = ScreenFixtures.session,
                    dueReviewCount = 12,
                    weakQuestionCount = 7,
                    dailyGoal = 20
                ),
                actions = HomeActions()
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("復習期限・弱点・未出題の順に20問（約12分）").performScrollTo().assertIsDisplayed()
        listOf("続きから再開", "今日のおすすめを開始", "今日の復習", "弱点克服", "章別学習", "周回モード", "用語カード", "模擬試験", "学習分析", "設定").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun settings() {
        val host = compose.setResponsiveContent(device) {
            SettingsContent(
                uiState = SettingsUiState(
                    isLoading = false,
                    preferences = UserPreferences(dailyGoalQuestions = 10, darkThemeConfig = DarkThemeConfig.DARK)
                ),
                onSelectTheme = {},
                onSelectDailyGoal = {},
                onOpenAbout = {}
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("システムに合わせる").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ダーク").performScrollTo().assertIsSelected()
        compose.onNodeWithText("ライト").performScrollTo().assertIsNotSelected()
        compose.onNodeWithText("10問").performScrollTo().assertIsSelected()
        compose.onNodeWithText("20問").performScrollTo().assertIsNotSelected()
        compose.onNodeWithText("このアプリについて").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun about() {
        val host = compose.setResponsiveContent(device) { AboutContent(versionName = "1.0.0") }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("バージョン 1.0.0").assertIsDisplayed()
        // What ADR-0006 requires the app to show.
        compose.onNodeWithText("非公式の学習アプリです").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("承認・推奨・認定を受けたものではありません", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("登録商標です", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Version 2023V4.0.J02（© ISTQB®、日本語翻訳 © JSTQB®）", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("インターネットに接続する権限を持たず", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("https://www.apache.org/licenses/LICENSE-2.0", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun quiz() {
        val host = compose.setResponsiveContent(device) {
            QuizContent(
                uiState = QuizUiState(
                    session = ScreenFixtures.session,
                    currentQuestion = ScreenFixtures.scenarioQuestion,
                    selectedChoiceIds = setOf("B")
                ),
                onSelectChoice = {},
                onSelectConfidence = {},
                onSubmit = {}
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("勘で選んだ").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("解答を確定する").performScrollTo().assertIsDisplayed()
        // A table row is one screen-reader node that pairs each value with its column header.
        compose.onNodeWithContentDescription("表の見出し。購入金額、ランク、割引率、ポイント付与率、備考").assertExists()
        compose.onNodeWithContentDescription("2行目。購入金額 0〜9,999、ランク 一般、割引率 0%、ポイント付与率 1%、備考 なし").assertExists()
        // positionInRoot is not clipped by scrolling, unlike boundsInRoot.
        val label = compose.onNodeWithText("FL-4.2.2", substring = true).fetchSemanticsNode()
        val choices = compose.onNodeWithText("選択肢（2つ選択）").fetchSemanticsNode()
        val labelRight = label.positionInRoot.x + label.size.width
        val labelBottom = label.positionInRoot.y + label.size.height
        if (device.isWide) {
            assertTrue("choices should be in a right-hand pane", choices.positionInRoot.x >= labelRight)
        } else {
            assertTrue("choices should be below the question", choices.positionInRoot.y >= labelBottom)
        }
    }

    @Test
    fun explanation() {
        val host = compose.setResponsiveContent(device) {
            ExplanationContent(
                uiState = ExplanationUiState(
                    session = ScreenFixtures.session,
                    question = ScreenFixtures.scenarioQuestion,
                    attempt = ScreenFixtures.wrongAttempt
                ),
                onSelectMistakeReason = {},
                onNext = {}
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("うっかりミス").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("次の問題へ").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun result() {
        val host = compose.setResponsiveContent(device) {
            ResultContent(
                uiState = ResultUiState(
                    totalQuestions = 5,
                    correctCount = 2,
                    accuracyPercentage = 40,
                    recommendedAction = RecommendedAction.WEAKNESS,
                    summary = ScreenFixtures.summary
                ),
                onStartRecommended = {},
                onNavigateHome = {}
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("所要時間 3分05秒").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("復習が必要な学習目的").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(RecommendedAction.WEAKNESS.buttonLabel).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ホームへ戻る").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun chapterSelect() {
        val host = compose.setResponsiveContent(device) {
            ChapterSelectContent(
                uiState = ChapterSelectUiState(
                    chapters = chapterOverview(emptyList(), emptyList(), emptyList())
                        .map { it.copy(questionCount = 63, unseenCount = 40, mastery = 0.55f) }
                ),
                onStartChapter = { _, _ -> }
            )
        }
        compose.assertNoHorizontalOverflow()
        compose.assertAccessible(host)
        compose.onNodeWithText("第5章 テスト活動のマネジメント").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("全63問").onLast().performScrollTo().assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun devices() = TestDevice.parameters()
    }
}
