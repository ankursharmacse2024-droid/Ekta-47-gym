package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val EktaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private val EktaLightColorScheme = lightColorScheme(
    primary = TurquoisePrimary,
    onPrimary = Color.White,
    primaryContainer = TurquoiseSoft,
    onPrimaryContainer = TurquoiseText,
    secondary = RoyalBlueAccent,
    onSecondary = Color.White,
    secondaryContainer = NavHighlightBg,
    onSecondaryContainer = NavActiveContent,
    tertiary = TopBarTurquoise,
    onTertiary = Color.White,
    tertiaryContainer = RoyalBlueSoft,
    onTertiaryContainer = TextPrimary,
    background = SurfaceCanvas,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = InputBorder,
    outlineVariant = BorderSubtle,
    error = StatusExpiredAccent,
    onError = Color.White,
    errorContainer = StatusExpiredBg,
    onErrorContainer = StatusExpiredText
)

@Composable
fun EktaGymTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EktaLightColorScheme,
        typography = Typography,
        shapes = EktaShapes,
        content = content
    )
}
