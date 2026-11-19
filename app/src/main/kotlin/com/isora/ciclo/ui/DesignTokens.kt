package com.isora.ciclo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colors from DESIGN.md. Nothing outside this set may be used in the UI. */
object CicloColors {
    val primary = Color(0xFF0A0A0A)
    val onPrimary = Color(0xFFFFFFFF)
    val brandGreen = Color(0xFF00D4A4)
    val brandGreenDeep = Color(0xFF00B48A)
    val brandGreenSoft = Color(0xFF7CEBCB)
    val brandTag = Color(0xFF3772CF)
    val brandWarn = Color(0xFFC37D0D)
    val brandAnnotate = Color(0xFF1BA673)
    val brandError = Color(0xFFD45656)
    val canvas = Color(0xFFFFFFFF)
    val surface = Color(0xFFF7F7F7)
    val surfaceSoft = Color(0xFFFAFAFA)
    val hairline = Color(0xFFE5E5E5)
    val hairlineSoft = Color(0xFFEDEDED)
    val ink = Color(0xFF0A0A0A)
    val charcoal = Color(0xFF1C1C1E)
    val slate = Color(0xFF3A3A3C)
    val steel = Color(0xFF5A5A5C)
    val stone = Color(0xFF888888)
    val muted = Color(0xFFA8A8AA)
}

/** Spacing scale from DESIGN.md. */
object CicloSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val sectionSm = 48.dp
    val section = 64.dp
}

/** Border radii from DESIGN.md. */
object CicloRadius {
    val xs = 4.dp
    val sm = 6.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 24.dp
}
