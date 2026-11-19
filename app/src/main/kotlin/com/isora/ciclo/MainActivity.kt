package com.isora.ciclo

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.isora.ciclo.notifications.FridayNotifier
import com.isora.ciclo.security.LockScreen
import com.isora.ciclo.security.PinStore
import com.isora.ciclo.ui.CicloTheme

class MainActivity : FragmentActivity() {
    private val container get() = (application as CicloApplication).container
    private val pins by lazy { PinStore(this) }
    private var locked by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep patient data out of screenshots and the recent-apps thumbnail.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        FridayNotifier.schedule(this)
        setContent {
            CicloTheme {
                Surface(Modifier.fillMaxSize()) {
                    if (locked) {
                        LockScreen(pins) { locked = false }
                    } else {
                        CicloApp(container, pins) { locked = true }
                    }
                }
            }
        }
    }

    /** The app locks every time it leaves the foreground, except while it waits for a file picker it opened. */
    override fun onStop() {
        super.onStop()
        if (!container.lockGuard.suspended) locked = true
    }
}
