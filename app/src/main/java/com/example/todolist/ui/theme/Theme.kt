package com.example.todolist.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalPriorityColors = staticCompositionLocalOf { PriorityColors.Standard }

private val DarkColorScheme = darkColorScheme(
    primary = Indigo200,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo100,
    secondary = Blue200,
    onSecondary = Blue800,
    background = SurfaceDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo700,
    onPrimary = SurfaceLight,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Blue600,
    onSecondary = SurfaceLight,
    background = SurfaceLight,
    surface = SurfaceLight,
)

val fontScaleValues = floatArrayOf(0.85f, 1.0f, 1.15f)

@Composable
private fun ColorScheme.animate(): ColorScheme {
    @Composable
    fun anim(color: Color, label: String) = animateColorAsState(color, tween(350), label).value
    return copy(
        primary            = anim(primary,            "cs_primary"),
        onPrimary          = anim(onPrimary,          "cs_onPrimary"),
        primaryContainer   = anim(primaryContainer,   "cs_primaryContainer"),
        onPrimaryContainer = anim(onPrimaryContainer, "cs_onPrimaryContainer"),
        secondary          = anim(secondary,          "cs_secondary"),
        onSecondary        = anim(onSecondary,        "cs_onSecondary"),
        background         = anim(background,         "cs_background"),
        surface            = anim(surface,            "cs_surface"),
        onBackground       = anim(onBackground,       "cs_onBackground"),
        onSurface          = anim(onSurface,          "cs_onSurface"),
        surfaceVariant     = anim(surfaceVariant,     "cs_surfaceVariant"),
        onSurfaceVariant   = anim(onSurfaceVariant,   "cs_onSurfaceVariant"),
        error              = anim(error,              "cs_error"),
        onError            = anim(onError,            "cs_onError"),
        outline            = anim(outline,            "cs_outline"),
    )
}

@Composable
fun ToDoListTheme(
    darkTheme: Boolean,
    fontScaleIndex: Int = 1,
    accentScheme: Int = 0,
    content: @Composable () -> Unit
) {
    val colorScheme = (if (darkTheme) DarkColorScheme else LightColorScheme).animate()
    val priorityColors = PriorityColors.fromScheme(accentScheme)
    val fontScale = fontScaleValues.getOrElse(fontScaleIndex) { 1.0f }
    val currentDensity = LocalDensity.current

    CompositionLocalProvider(
        LocalPriorityColors provides priorityColors,
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = fontScale
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
