package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FgPrimaryGreen,
    onPrimary = FgDarkGreen,
    secondary = FgMint,
    onSecondary = FgDarkGreen,
    tertiary = FgEmerald,
    background = FgBackgroundDark,
    surface = FgSurfaceDark,
    onBackground = FgTextPrimaryDark,
    onSurface = FgTextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = FgPrimaryGreen,
    onPrimary = Color.White,
    secondary = FgDarkGreen,
    onSecondary = Color.White,
    tertiary = FgMint,
    background = FgBackgroundLight,
    surface = FgSurfaceLight,
    onBackground = FgTextPrimaryLight,
    onSurface = FgTextPrimaryLight
)

@Composable
fun FgTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = FgTimeTheme(darkTheme, dynamicColor, content)
