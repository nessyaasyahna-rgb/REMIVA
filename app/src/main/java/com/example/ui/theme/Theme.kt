package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FarmivaSageAccent,
    onPrimary = Color.Black,
    primaryContainer = FarmivaDarkMintContainer,
    onPrimaryContainer = FarmivaMintSoft,
    secondary = FarmivaMintSoft,
    onSecondary = Color.Black,
    secondaryContainer = FarmivaDarkBorder,
    onSecondaryContainer = Color.White,
    tertiary = FarmivaCreamSecondary,
    background = FarmivaDarkBg,
    onBackground = TextLightPrimary,
    surface = FarmivaDarkSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = Color(0xFF22352B),
    onSurfaceVariant = TextLightSecondary,
    outline = FarmivaDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FarmivaPrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = FarmivaMintSoft,
    onPrimaryContainer = FarmivaForestGreen,
    secondary = FarmivaForestGreen,
    onSecondary = Color.White,
    secondaryContainer = FarmivaCreamSecondary,
    onSecondaryContainer = TextDarkPrimary,
    tertiary = FarmivaSageAccent,
    onTertiary = Color.White,
    background = FarmivaCreamBg,
    onBackground = TextDarkPrimary,
    surface = FarmivaCreamCard,
    onSurface = TextDarkPrimary,
    surfaceVariant = FarmivaLightMint,
    onSurfaceVariant = TextDarkSecondary,
    outline = FarmivaCreamBorder
)

@Composable
fun FarmivaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) = FarmivaTheme(darkTheme = darkTheme, content = content)
