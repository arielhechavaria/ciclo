package com.isora.ciclo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Marks drawn on the timeline and its legend. */
object CicloPalette {
    val cycle = Color(0xFFCCEBE7)
    val session = Color(0xFF0F766E)
    val sessionGiven = Color(0xFF7FB5AF)
    val lastSession = Color(0xFF15803D)
    val postponed = Color(0xFFD97706)
    val today = Color(0xFFDC2626)
}

private val scheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    primaryContainer = Color(0xFFCCEBE7),
    onPrimaryContainer = Color(0xFF053B37),
    secondary = Color(0xFF475569),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFF8FAFC),
)

@Composable
fun CicloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

private val spanish = Locale("es")
private val longDate = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", spanish)

fun LocalDate.formatLong(): String = longDate.format(this)
fun LocalDate.formatShort(): String = shortDate.format(this).replaceFirstChar { it.uppercase() }
