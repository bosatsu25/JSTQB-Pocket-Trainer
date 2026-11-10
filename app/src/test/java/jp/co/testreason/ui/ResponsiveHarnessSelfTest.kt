package jp.co.testreason.ui

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Proves the responsive assertions fail on broken layouts, so a green screen test means something. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1400dp-h1400dp")
// Real text measurement: in LEGACY mode text has almost no width, which would hide overflow.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResponsiveHarnessSelfTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun overflowingElement_isReported() {
        compose.setResponsiveContent(TestDevice.SMALL_PHONE) {
            Column { Text("横に長すぎる要素", modifier = Modifier.requiredWidth(500.dp)) }
        }
        assertThrows(AssertionError::class.java) { compose.assertNoHorizontalOverflow() }
    }

    @Test
    fun overflowInsideVerticalScroll_isReported() {
        compose.setResponsiveContent(TestDevice.SMALL_PHONE) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("スクロール内の長い要素", modifier = Modifier.requiredWidth(500.dp))
            }
        }
        assertThrows(AssertionError::class.java) { compose.assertNoHorizontalOverflow() }
    }

    @Test
    fun clickableWithoutAName_isReported() {
        compose.setResponsiveContent(TestDevice.PHONE) {
            Box(modifier = Modifier.size(48.dp).clickable { })
        }
        assertThrows(AssertionError::class.java) { compose.assertInteractiveElementsAreLabelled() }
    }

    @Test
    fun selectableWithoutARole_isReported() {
        compose.setResponsiveContent(TestDevice.PHONE) {
            Box(modifier = Modifier.size(48.dp).selectable(selected = true, onClick = { })) { Text("選択肢A") }
        }
        assertThrows(AssertionError::class.java) { compose.assertInteractiveElementsAreLabelled() }
    }

    @Test
    fun namedButtonAndRadioChoice_pass() {
        compose.setResponsiveContent(TestDevice.PHONE) {
            Column {
                Box(modifier = Modifier.size(48.dp).clickable { }) { Text("次へ") }
                Box(modifier = Modifier.size(48.dp).selectable(selected = true, role = Role.RadioButton, onClick = { })) { Text("選択肢A") }
            }
        }
        compose.assertInteractiveElementsAreLabelled()
    }

    @Test
    fun smallTouchTarget_isReported() {
        val host = compose.setResponsiveContent(TestDevice.PHONE) {
            Box(modifier = Modifier.size(24.dp).semantics { contentDescription = "小さいボタン" }.clickable { })
        }
        assertThrows(AssertionError::class.java) { compose.assertTouchTargetsAtLeast48dp(host) }
    }
}
