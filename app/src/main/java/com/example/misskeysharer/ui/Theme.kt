package com.example.misskeysharer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MisskeyGreen = Color(0xFF86B300)

private val LightColors = lightColorScheme(
    primary = MisskeyGreen,
    onPrimary = Color.White,
    secondary = MisskeyGreen,
    onSecondary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = MisskeyGreen,
    onPrimary = Color.Black,
    secondary = MisskeyGreen,
    onSecondary = Color.Black
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
