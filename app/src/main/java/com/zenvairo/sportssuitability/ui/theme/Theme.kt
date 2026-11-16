package com.zenvairo.sportssuitability.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ocean = Color(0xFF0B72B9)
private val OceanDark = Color(0xFF7FC3EE)
private val Snow = Color(0xFF4FC3F7)

private val LightColors = lightColorScheme(
    primary = Ocean,
    secondary = Snow
)

private val DarkColors = darkColorScheme(
    primary = OceanDark,
    secondary = Snow
)

@Composable
fun SportsSuitabilityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
