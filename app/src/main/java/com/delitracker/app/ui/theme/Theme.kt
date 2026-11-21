package com.delitracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores adicionales del prototipo que no tienen un slot directo en Material3 ColorScheme. */
data class DtExtendedColors(
    val surface2: Color,
    val textMuted: Color,
    val accentSoft: Color,
    val accentSoftInk: Color,
    val accent2: Color,
    val accent2Ink: Color,
    val accent2Soft: Color,
    val accent2SoftInk: Color,
    val border: Color,
    val warning: Color
)

private val LightExtended = DtExtendedColors(
    surface2 = DtSurface2,
    textMuted = DtTextMuted,
    accentSoft = DtAccentSoft,
    accentSoftInk = DtAccentSoftInk,
    accent2 = DtAccent2,
    accent2Ink = DtAccent2Ink,
    accent2Soft = DtAccent2Soft,
    accent2SoftInk = DtAccent2SoftInk,
    border = DtBorder,
    warning = DtWarning
)

private val DarkExtended = DtExtendedColors(
    surface2 = DtSurface2Dark,
    textMuted = DtTextMutedDark,
    accentSoft = DtAccentSoftDark,
    accentSoftInk = DtAccentSoftInkDark,
    accent2 = DtAccent2Dark,
    accent2Ink = DtAccent2InkDark,
    accent2Soft = DtAccent2SoftDark,
    accent2SoftInk = DtAccent2SoftInkDark,
    border = DtBorderDark,
    warning = DtWarningDark
)

val LocalDtColors = staticCompositionLocalOf { LightExtended }

private val LightScheme = lightColorScheme(
    primary = DtAccent,
    onPrimary = DtAccentInk,
    secondary = DtAccent2,
    onSecondary = DtAccent2Ink,
    background = DtBackground,
    onBackground = DtText,
    surface = DtSurface,
    onSurface = DtText,
    surfaceVariant = DtSurface2,
    onSurfaceVariant = DtText2,
    error = DtDanger,
    errorContainer = DtDangerSoft,
    outline = DtBorder
)

private val DarkScheme = darkColorScheme(
    primary = DtAccentDark,
    onPrimary = DtAccentInkDark,
    secondary = DtAccent2Dark,
    onSecondary = DtAccent2InkDark,
    background = DtBackgroundDark,
    onBackground = DtTextDark,
    surface = DtSurfaceDark,
    onSurface = DtTextDark,
    surfaceVariant = DtSurface2Dark,
    onSurfaceVariant = DtText2Dark,
    error = DtDangerDark,
    errorContainer = DtDangerSoftDark,
    outline = DtBorderDark
)

object DtTheme {
    val colors: DtExtendedColors
        @Composable get() = LocalDtColors.current
}

@Composable
fun DeliTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(LocalDtColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DtTypography,
            content = content
        )
    }
}
