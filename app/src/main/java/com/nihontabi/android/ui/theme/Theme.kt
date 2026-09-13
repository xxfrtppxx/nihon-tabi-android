package com.nihontabi.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors the web app has (dot-dim, dot-coast, the amber "plan" scale) that
 * don't map onto a Material3 [androidx.compose.material3.ColorScheme] slot.
 */
data class NihonTabiExtendedColors(
    val plan: Color,
    val planContainer: Color,
    val onPlanContainer: Color,
    val dotDim: Color,
    val dotCoast: Color,
    val divider: Color,
)

private val LightExtendedColors = NihonTabiExtendedColors(
    plan = Plan,
    planContainer = Plan100,
    onPlanContainer = Plan700,
    dotDim = LightDotDim,
    dotCoast = LightDotCoast,
    divider = LightDivider,
)

private val DarkExtendedColors = NihonTabiExtendedColors(
    plan = Plan,
    planContainer = Plan100,
    onPlanContainer = Plan700,
    dotDim = DarkDotDim,
    dotCoast = DarkDotCoast,
    divider = DarkDivider,
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: NihonTabiExtendedColors
    @Composable
    get() = LocalExtendedColors.current

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Accent100,
    onPrimaryContainer = Accent700,
    secondary = Accent500,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightSurface,
    onSurface = LightForeground,
    surfaceVariant = LightNeutral200,
    onSurfaceVariant = LightNeutral600,
    outline = LightNeutral400,
    outlineVariant = LightNeutral300,
    error = Plan700,
)

private val DarkColors = darkColorScheme(
    primary = Accent500,
    onPrimary = Color.White,
    primaryContainer = Accent700,
    onPrimaryContainer = Accent100,
    secondary = Accent500,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkSurface,
    onSurface = DarkForeground,
    surfaceVariant = DarkNeutral200,
    onSurfaceVariant = DarkNeutral600,
    outline = DarkNeutral400,
    outlineVariant = DarkNeutral300,
    error = Plan,
)

@Composable
fun NihonTabiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NihonTabiTypography,
            content = content,
        )
    }
}
