package jp.co.testreason.e2e

import android.os.Looper
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf

/**
 * Runs the tasks still queued on the main looper after a test that drives a real activity.
 * Use it as the outermost rule: `@get:Rule(order = 0)`, with the compose rule at `order = 1`.
 *
 * Without it, a test that destroys its activity itself (ProcessRecreationTest) breaks later Compose tests in
 * the same JVM: any test that changes state and then calls `waitForIdle()` fails with "Compose did not get
 * idle" (verified: removing the drain makes DrillScreensResponsiveTest fail, restoring it fixes it).
 *
 * Likely mechanism: Compose keeps one process-wide notifier for writes to snapshot state. It schedules its work
 * on the main looper and sends nothing more until that work has run. Robolectric drops queued main-looper
 * tasks when a test ends, so a task dropped there leaves the notifier waiting forever, in every later test.
 */
class DrainMainLooperRule : ExternalResource() {
    override fun after() {
        shadowOf(Looper.getMainLooper()).idle()
    }
}
