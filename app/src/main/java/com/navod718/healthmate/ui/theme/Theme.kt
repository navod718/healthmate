package com.navod718.healthmate.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF146B5B), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F2E8), onPrimaryContainer = Color(0xFF0C493D),
    secondary = Color(0xFF4A635B), background = Color(0xFFF7FAF7),
    surface = Color(0xFFF7FAF7), surfaceVariant = Color(0xFFE8EFEA),
)
@Composable
fun HealthMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Typography(), content = content)
}
