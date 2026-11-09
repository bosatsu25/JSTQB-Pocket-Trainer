package jp.co.testreason.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import jp.co.testreason.core.ui.theme.TestReasonTheme
import org.junit.Assert.fail

/** Screen configurations every screen must handle (docs/ux/ACCESSIBILITY.md, docs/quality/TEST_STRATEGY.md). */
enum class TestDevice(val width: Dp, val height: Dp, val fontScale: Float) {
    SMALL_PHONE(320.dp, 568.dp, 1f),
    SMALL_PHONE_FONT_200(320.dp, 568.dp, 2f),
    PHONE(411.dp, 891.dp, 1f),
    PHONE_LANDSCAPE(891.dp, 411.dp, 1f),
    TABLET_LANDSCAPE(1280.dp, 800.dp, 1f);

    val isWide: Boolean get() = width >= 720.dp

    companion object {
        fun parameters(): List<Array<Any>> = entries.map { arrayOf(it) }
    }
}

private const val SCREEN_TAG = "responsive-screen"
private const val MIN_TOUCH_TARGET_DP = 48f
private const val TOLERANCE_PX = 1f

/** Holds the density seen inside the overridden configuration, to convert node sizes to dp. */
class ResponsiveHost {
    lateinit var density: Density
}

/** Renders [content] as if on [device] (size and font scale) inside the app theme. */
fun ComposeContentTestRule.setResponsiveContent(device: TestDevice, content: @Composable () -> Unit): ResponsiveHost {
    val host = ResponsiveHost()
    setContent {
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.ForcedSize(DpSize(device.width, device.height)) then
                DeviceConfigurationOverride.FontScale(device.fontScale)
        ) {
            host.density = LocalDensity.current
            TestReasonTheme(dynamicColor = false) {
                Box(modifier = Modifier.fillMaxSize().testTag(SCREEN_TAG)) { content() }
            }
        }
    }
    return host
}

/**
 * Fails when any element sticks out of the screen horizontally. Content inside an intentionally
 * horizontally scrollable container (tables, code) is skipped.
 */
fun ComposeContentTestRule.assertNoHorizontalOverflow() {
    val screen = onNodeWithTag(SCREEN_TAG, useUnmergedTree = true).fetchSemanticsNode()
    val screenLeft = screen.positionInRoot.x
    val screenRight = screenLeft + screen.size.width
    val offenders = mutableListOf<String>()
    fun visit(node: SemanticsNode) {
        if (node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null) return
        // Unclipped geometry: boundsInRoot is clipped by scroll containers and would hide overflow.
        val left = node.positionInRoot.x
        val right = left + node.size.width
        if (node.size.width > 0 && (left < screenLeft - TOLERANCE_PX || right > screenRight + TOLERANCE_PX)) {
            offenders += "${node.describe()} spans $left..$right, screen $screenLeft..$screenRight"
        }
        node.children.forEach(::visit)
    }
    visit(screen)
    if (offenders.isNotEmpty()) fail("Horizontal overflow:\n" + offenders.joinToString("\n"))
}

/** Fails when a clickable element's touch area is smaller than 48dp x 48dp. */
fun ComposeContentTestRule.assertTouchTargetsAtLeast48dp(host: ResponsiveHost) {
    val screen = onNodeWithTag(SCREEN_TAG, useUnmergedTree = true).fetchSemanticsNode()
    val minPx = with(host.density) { MIN_TOUCH_TARGET_DP.dp.toPx() } - TOLERANCE_PX
    val offenders = mutableListOf<String>()
    fun visit(node: SemanticsNode) {
        if (node.config.getOrNull(SemanticsActions.OnClick) != null) {
            // Layout size, not on-screen bounds: elements scrolled out of view must be checked too.
            val size = node.size
            if (size.width < minPx || size.height < minPx) {
                val w = size.width / host.density.density
                val h = size.height / host.density.density
                offenders += "${node.describe()} is ${w}dp x ${h}dp"
            }
        }
        node.children.forEach(::visit)
    }
    visit(screen)
    if (offenders.isNotEmpty()) fail("Touch targets below 48dp:\n" + offenders.joinToString("\n"))
}

private fun SemanticsNode.describe(): String {
    val text = config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
        ?: children.firstNotNullOfOrNull { child -> child.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text } }
    return "node#$id(${text ?: "no text"})"
}
