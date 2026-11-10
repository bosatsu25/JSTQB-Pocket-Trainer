package jp.co.testreason.e2e

import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The about screen tells the learner that the app cannot connect to the internet. That is only true while
 * no dependency or change adds a network permission to the merged manifest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppManifestTest {

    @Test
    fun theApp_requestsNoNetworkPermission() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toList()

        // Guard against a vacuous pass: the merged manifest does declare this permission (from androidx.core).
        assertTrue("The merged manifest was not read: $requested", requested.any { it.endsWith(KNOWN_DECLARED_PERMISSION) })

        val network = requested.filter { it in NETWORK_PERMISSIONS }
        assertTrue("Network permissions in the merged manifest: $network (all: $requested)", network.isEmpty())
    }

    private companion object {
        const val KNOWN_DECLARED_PERMISSION = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        val NETWORK_PERMISSIONS = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE"
        )
    }
}
