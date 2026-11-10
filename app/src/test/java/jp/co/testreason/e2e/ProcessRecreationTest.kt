package jp.co.testreason.e2e

import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.GraphicsMode

/**
 * What survives when Android kills the app in the background and the learner comes back: the activity is
 * destroyed and a new one is built from the saved instance state only. ViewModels are new, so anything
 * that is still on screen afterwards came from the navigation state, SavedStateHandle or the database.
 * (The process itself stays, so singletons such as the database connection are the same objects.)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProcessRecreationTest {

    @get:Rule(order = 0)
    val drainMainLooper = DrainMainLooperRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val app = AppRobot(compose)
    private lateinit var activity: ActivityController<MainActivity>

    // Launched here, not in a field initialiser: the compose rule must be running before setContent is called.
    @Before
    fun launch() {
        activity = Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @After
    fun tearDown() {
        activity.pause().stop().destroy()
    }

    private fun killAndRestore() {
        compose.waitForIdle()
        val state = Bundle()
        activity.saveInstanceState(state).pause().stop().destroy()
        activity = Robolectric.buildActivity(MainActivity::class.java).setup(state)
    }

    @Test
    fun quiz_keepsTheQuestionTheSelectionAndTheConfidence() {
        app.tap("今日のおすすめを開始")
        app.waitFor("問題 1 / 5")
        app.waitFor("解答を確定する")
        val isMulti = app.has("つ選択", substring = true)
        app.selectChoices()
        app.tap("勘で選んだ")

        killAndRestore()

        app.waitFor("問題 1 / 5")
        app.waitFor("解答を確定する")
        if (isMulti) {
            repeat(2) { compose.onAllNodes(isToggleable())[it].assertIsOn() }
        } else {
            compose.onAllNodes(isSelectable())[0].assertIsSelected()
        }
        compose.onNodeWithText("勘で選んだ").performScrollTo().assertIsSelected()
        compose.onNodeWithText("解答を確定する").performScrollTo().assertIsEnabled()

        // The restored input can be submitted as it is.
        app.tap("解答を確定する")
        app.waitFor("解答・解説")
    }

    @Test
    fun mockExam_keepsTheAnswersAndTheClock() {
        app.tap("模擬試験")
        app.tap("模擬試験を開始")
        app.waitFor("問題 1 / 40")
        app.selectChoices()
        app.waitFor("回答済み 1 / 40", substring = true)

        killAndRestore()

        app.waitFor("問題 1 / 40")
        app.waitFor("回答済み 1 / 40", substring = true)
        // Still counting down from the original start, not from a fresh 60:00.
        app.waitFor("残り 59:", substring = true)
        compose.onNodeWithText("提出する").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settings_areStillThereAfterTheAppIsKilled() {
        app.tap("設定")
        app.tap("20問")
        app.tap("ライト")
        compose.waitUntil(TIMEOUT_MS) { runCatching { compose.onNodeWithText("ライト").assertIsSelected() }.isSuccess }

        killAndRestore()

        app.waitFor("1日の目標問題数")
        compose.onNodeWithText("20問").assertIsSelected()
        compose.onNodeWithText("ライト").assertIsSelected()
    }

    @Test
    fun rapidDrill_keepsTheLapAndTheShownAnswer() {
        app.tap("周回モード")
        app.waitFor("全範囲から")
        app.tap("40問")
        app.waitFor("周回モード 0 / 40 クリア")
        app.tap("分からないので答えを見る")
        app.waitFor("答えを確認")

        killAndRestore()

        app.waitFor("周回モード 0 / 40 クリア")
        app.waitFor("答えを確認")
        compose.onNodeWithText("次へ").performScrollTo().assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
