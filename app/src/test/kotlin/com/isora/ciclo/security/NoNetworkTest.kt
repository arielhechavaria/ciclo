package com.isora.ciclo.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The app is fully offline: the merged manifest (with every library's) must not ask for network access. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoNetworkTest {
    @Test
    fun theAppRequestsNoNetworkPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toList()

        // Control: this is the merged manifest, not an empty one.
        assertTrue(requested.toString(), "android.permission.POST_NOTIFICATIONS" in requested)
        assertFalse(requested.toString(), "android.permission.INTERNET" in requested)
        assertFalse(requested.toString(), "android.permission.ACCESS_NETWORK_STATE" in requested)
    }
}
