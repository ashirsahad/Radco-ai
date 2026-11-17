package com.accountpro.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TuscanRed,
    onPrimary = PureWhite,
    primaryContainer = TuscanRedSoft,
    onPrimaryContainer = TuscanRedDark,
    secondary = JetBlack,
    onSecondary = PureWhite,
    secondaryContainer = SubtleSurface,
    onSecondaryContainer = CharcoalInk,
    tertiary = MutedInk,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = JetBlack,
    surface = PureWhite,
    onSurface = JetBlack,
    surfaceVariant = MutedGraySurface,
    onSurfaceVariant = MutedInk,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SoftPaper,
    surfaceContainer = MutedGraySurface,
    surfaceContainerHigh = SubtleSurface,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = TuscanRed,
    onError = PureWhite,
    errorContainer = TuscanRedSoft,
    onErrorContainer = TuscanRedDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = TuscanRed,
    onPrimary = PureWhite,
    primaryContainer = TuscanRedDark,
    onPrimaryContainer = PureWhite,
    secondary = CrispOffWhite,
    onSecondary = JetBlack,
    secondaryContainer = ElevatedDarkSurface,
    onSecondaryContainer = CrispOffWhite,
    tertiary = DarkMutedText,
    onTertiary = JetBlack,
    background = ObsidianBackground,
    onBackground = CrispOffWhite,
    surface = CharcoalSurface,
    onSurface = CrispOffWhite,
    surfaceVariant = ElevatedDarkSurface,
    onSurfaceVariant = DarkMutedText,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = ElevatedDarkSurface,
    surfaceContainerHigh = DarkBorder,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = TuscanRedSoft,
    onError = TuscanRedDark,
    errorContainer = TuscanRed,
    onErrorContainer = PureWhite,
)

@Composable
fun AccountProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
