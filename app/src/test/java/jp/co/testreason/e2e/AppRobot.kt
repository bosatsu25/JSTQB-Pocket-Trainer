package jp.co.testreason.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/** Drives the app the way a learner does: by the texts on screen. Waits for asynchronous loads. */
class AppRobot(private val compose: ComposeTestRule) : SemanticsNodeInteractionsProvider by compose {

    fun has(text: String, substring: Boolean = false): Boolean =
        onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    fun waitFor(text: String, substring: Boolean = false) {
        try {
            compose.waitUntil(TIMEOUT_MS) { has(text, substring) }
        } catch (e: ComposeTimeoutException) {
            // Say what was on screen instead: a bare timeout cannot be diagnosed from a CI log.
            val onScreen = onAllNodes(hasText("", substring = true)).fetchSemanticsNodes()
                .flatMap { node -> node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } }
            throw AssertionError("Timed out waiting for \"$text\". On screen: $onScreen", e)
        }
    }

    fun tap(text: String) {
        waitFor(text)
        onNodeWithText(text).performScrollTo().performClick()
    }

    /**
     * Picks the first choice of the question on screen, or the first two when it asks for two.
     * Choices are the only toggleable nodes, and the first selectable ones (confidence chips come after).
     */
    fun selectChoices() {
        if (has("つ選択", substring = true)) {
            repeat(2) { index -> onAllNodes(isToggleable())[index].performScrollTo().performClick() }
        } else {
            onAllNodes(isSelectable())[0].performScrollTo().performClick()
        }
    }

    fun answerQuizQuestion(number: Int, total: Int) {
        waitFor("問題 $number / $total")
        waitFor("解答を確定する")
        selectChoices()
        tap("解答を確定する")
        waitFor("解答・解説")
    }

    /** The drill and the cards ignore taps for 400 ms after the content changed (real clock in this test). */
    fun waitOutTheTapGuard() = Thread.sleep(TAP_GUARD_MS)

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val TAP_GUARD_MS = 450L
    }
}
