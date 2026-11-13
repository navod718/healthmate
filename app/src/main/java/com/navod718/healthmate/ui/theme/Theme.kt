package com.navod718.healthmate.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navod718.healthmate.R

internal val BrandIndigo = Color(0xFF263B68)
internal val BrandMint = Color(0xFF8EE5D3)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006872), onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F2EF), onPrimaryContainer = Color(0xFF00363B),
    secondary = Color(0xFF465E77), secondaryContainer = Color(0xFFDCE9FC),
    onSecondaryContainer = Color(0xFF182F4A),
    tertiary = BrandIndigo,
    inverseSurface = Color(0xFF263536), inverseOnSurface = Color(0xFFEAF3F1), inversePrimary = Color(0xFF85DAD8),
    background = Color(0xFFF5FAF9), onBackground = Color(0xFF132829),
    surface = Color(0xFFF5FAF9), onSurface = Color(0xFF132829),
    surfaceContainer = Color(0xFFEAF3F1), surfaceContainerLow = Color(0xFFFCFEFD),
    surfaceContainerHigh = Color(0xFFE2EDEB), surfaceVariant = Color(0xFFE0EDEB),
    onSurfaceVariant = Color(0xFF455A5C), outline = Color(0xFF6F8385), outlineVariant = Color(0xFFCCDCD9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF85DAD8), onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF004F55), onPrimaryContainer = Color(0xFFC6F2EF),
    secondary = Color(0xFFB8CCE7), secondaryContainer = Color(0xFF2C4560),
    onSecondaryContainer = Color(0xFFDCE9FC),
    tertiary = Color(0xFFC0D0FF),
    inverseSurface = Color(0xFFDFEDEB), inverseOnSurface = Color(0xFF203338), inversePrimary = Color(0xFF006872),
    background = Color(0xFF101C20), onBackground = Color(0xFFE7F2F1),
    surface = Color(0xFF101C20), onSurface = Color(0xFFE7F2F1),
    surfaceContainer = Color(0xFF17272B), surfaceContainerLow = Color(0xFF152429),
    surfaceContainerHigh = Color(0xFF203338), surfaceVariant = Color(0xFF293D41),
    onSurfaceVariant = Color(0xFFB9CDCC), outline = Color(0xFF8CA4A4), outlineVariant = Color(0xFF3E5356),
)

private val defaults = Typography()
private val HealthTypography = Typography(
    headlineMedium = defaults.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
    titleLarge = defaults.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    labelLarge = defaults.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun HealthMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = HealthTypography,
        content = content,
    )
}

@Composable
internal fun HealthMateMark() {
    Box(
        Modifier.size(36.dp).background(BrandIndigo, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.healthmate_foreground), contentDescription = null, Modifier.size(40.dp))
    }
}
