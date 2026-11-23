package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val VaultLightColorScheme = lightColorScheme(
    primary = VaultElectricBlue,
    onPrimary = VaultWhite,
    primaryContainer = VaultElectricBlueSoft,
    onPrimaryContainer = VaultNavy,
    secondary = VaultEmerald,
    onSecondary = VaultWhite,
    secondaryContainer = VaultEmeraldSoft,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = VaultNavy,
    onTertiary = VaultWhite,
    background = VaultLightGray,
    onBackground = VaultNavy,
    surface = VaultWhite,
    onSurface = VaultNavy,
    surfaceVariant = Color(0xFFEAF0F7),
    onSurfaceVariant = VaultSlateText,
    outline = VaultBorderLight,
    outlineVariant = Color(0xFFCBD5E1),
    error = VaultCoralRed,
    onError = VaultWhite,
    errorContainer = VaultCoralRedSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val VaultDarkColorScheme = darkColorScheme(
    primary = VaultElectricBlue,
    onPrimary = VaultWhite,
    primaryContainer = Color(0xFF153B6E),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = VaultEmerald,
    onSecondary = VaultWhite,
    secondaryContainer = Color(0xFF0E4434),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = VaultWhite,
    onTertiary = VaultNavy,
    background = VaultNavy,
    onBackground = VaultWhite,
    surface = VaultNavyElevated,
    onSurface = VaultWhite,
    surfaceVariant = VaultNavySurfaceHigh,
    onSurfaceVariant = Color(0xFFB3C3E0),
    outline = VaultBorderDark,
    outlineVariant = Color(0xFF1B2E54),
    error = VaultCoralRed,
    onError = VaultWhite,
    errorContainer = Color(0xFF4C1418),
    onErrorContainer = Color(0xFFFECACA)
)

val VaultShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode(val labelPt: String) {
    SYSTEM("Seguir Sistema"),
    LIGHT("Claro Premium"),
    DARK("Azul-Marinho Escuro")
}

@Composable
fun InternetVaultTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) VaultDarkColorScheme else VaultLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content
    )
}
