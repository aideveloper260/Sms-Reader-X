package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleContainer,
    onPrimaryContainer = BrandOnPurpleContainer,
    secondary = BrandPurpleLight,
    onSecondary = Color.White,
    secondaryContainer = BrandPurpleContainer,
    onSecondaryContainer = BrandOnPurpleContainer,
    tertiary = BrandPurpleDark,
    background = BrandBackground,
    onBackground = BrandTextPrimary,
    surface = BrandSurface,
    onSurface = BrandTextPrimary,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandTextSecondary,
    outline = BrandOutline,
    error = BrandError,
    onError = Color.White,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandError
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurpleLight,
    onPrimary = Color.Black,
    primaryContainer = BrandPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = BrandPurplePrimary,
    onSecondary = Color.White,
    background = Color(0xFF130E20),
    onBackground = Color(0xFFEDE9FE),
    surface = Color(0xFF1C162E),
    onSurface = Color(0xFFEDE9FE),
    surfaceVariant = Color(0xFF28203E),
    onSurfaceVariant = Color(0xFFCBC4DE),
    outline = Color(0xFF453A5F),
    error = Color(0xFFF87171),
    onError = Color.Black
)

@Composable
fun SMSReaderXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We intentionally pin the signature SMS READER X purple identity across all Android versions
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
