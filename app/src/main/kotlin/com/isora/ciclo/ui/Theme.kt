package com.isora.ciclo.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Marks drawn on the timeline and its legend, built only from DESIGN.md colors. */
object CicloPalette {
    val cycle = CicloColors.brandGreenSoft.copy(alpha = 0.4f)
    val session = CicloColors.brandGreenDeep
    val sessionGiven = CicloColors.brandGreenSoft
    val lastSession = CicloColors.brandAnnotate
    val postponed = CicloColors.brandWarn
    val today = CicloColors.brandError
}

private val scheme = lightColorScheme(
    primary = CicloColors.primary,
    onPrimary = CicloColors.onPrimary,
    primaryContainer = CicloColors.brandGreen,
    onPrimaryContainer = CicloColors.primary,
    secondary = CicloColors.steel,
    onSecondary = CicloColors.onPrimary,
    secondaryContainer = CicloColors.surface,
    onSecondaryContainer = CicloColors.ink,
    tertiary = CicloColors.brandWarn,
    onTertiary = CicloColors.onPrimary,
    background = CicloColors.canvas,
    onBackground = CicloColors.ink,
    surface = CicloColors.canvas,
    onSurface = CicloColors.ink,
    surfaceVariant = CicloColors.surface,
    onSurfaceVariant = CicloColors.steel,
    surfaceTint = Color.Transparent,
    outline = CicloColors.hairline,
    outlineVariant = CicloColors.hairlineSoft,
    error = CicloColors.brandError,
    onError = CicloColors.onPrimary,
)

/** Type scale from DESIGN.md. Inter is not bundled, so the documented substitute (system sans-serif) is used. */
private fun style(size: Int, ratio: Float, weight: FontWeight, spacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    lineHeight = (size * ratio).sp,
    fontWeight = weight,
    letterSpacing = spacing.sp,
)

private val typography = Typography(
    displayLarge = style(56, 1.1f, FontWeight.SemiBold, -1.5f),
    displayMedium = style(48, 1.1f, FontWeight.SemiBold, -1f),
    displaySmall = style(36, 1.2f, FontWeight.SemiBold, -0.5f),
    headlineLarge = style(36, 1.2f, FontWeight.SemiBold, -0.5f),
    headlineMedium = style(28, 1.25f, FontWeight.SemiBold),
    headlineSmall = style(22, 1.3f, FontWeight.SemiBold),
    titleLarge = style(22, 1.3f, FontWeight.SemiBold),
    titleMedium = style(18, 1.4f, FontWeight.SemiBold),
    titleSmall = style(14, 1.5f, FontWeight.Medium),
    bodyLarge = style(16, 1.5f, FontWeight.Normal),
    bodyMedium = style(14, 1.5f, FontWeight.Normal),
    bodySmall = style(13, 1.4f, FontWeight.Normal),
    labelLarge = style(14, 1.3f, FontWeight.Medium),
    labelMedium = style(12, 1.4f, FontWeight.Medium),
    labelSmall = style(11, 1.4f, FontWeight.Medium, 0.5f),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(CicloRadius.md),
    small = RoundedCornerShape(CicloRadius.md),
    medium = RoundedCornerShape(CicloRadius.lg),
    large = RoundedCornerShape(CicloRadius.xl),
    extraLarge = RoundedCornerShape(CicloRadius.xxl),
)

@Composable
fun CicloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}

private val spanish = Locale("es")
private val longDate = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", spanish)

fun LocalDate.formatLong(): String = longDate.format(this)
fun LocalDate.formatShort(): String = shortDate.format(this).replaceFirstChar { it.uppercase() }
