package com.musx.a1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = AccentYellow,
    background = BackgroundWhite,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onSecondary = PrimaryBlue,
    onTertiary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun MusxA1Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
