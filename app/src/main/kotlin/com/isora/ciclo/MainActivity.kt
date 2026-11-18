package com.isora.ciclo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.isora.ciclo.protocols.ProtocolCatalogScreen
import com.isora.ciclo.protocols.RoomProtocolCatalog
import com.isora.ciclo.storage.CicloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val database by lazy { CicloDatabase.open(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Temporary home until the navigation bar arrives (T8): the protocol catalog.
                    val catalog = remember { RoomProtocolCatalog(database.protocolDao()) }
                    var ready by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) { catalog.seedIfEmpty() }
                        ready = true
                    }
                    if (ready) ProtocolCatalogScreen(catalog)
                }
            }
        }
    }
}
