package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = Color(0xFFEF9A9A),
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color(0xFF00332E),
    onSecondary = Color(0xFF3B0000),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    tertiary = Color(0xFFB0BEC5)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = Color(0xFFC62828),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    tertiary = Color(0xFF607D8B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
