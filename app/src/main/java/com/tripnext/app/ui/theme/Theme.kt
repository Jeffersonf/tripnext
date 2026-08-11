package com.tripnext.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tripnext.app.R

enum class TripVisualTheme(val label: String, val storageKey: String) {
    BOARDING("Embarque", "boarding"), MODERN_DARK("Modern · Escuro", "modern_dark"),
    MODERN_LIGHT("Modern · Claro", "modern_light"), CLASSIC_DARK("Classic · Escuro", "classic_dark"),
    CLASSIC_LIGHT("Classic · Claro", "classic_light"), WEB_DARK("Web · Escuro", "web_dark"),
    WEB_LIGHT("Web · Claro", "web_light");
    companion object { fun fromKey(key: String?) = entries.firstOrNull { it.storageKey == key } ?: BOARDING }
}

private val TripColors = darkColorScheme(
    primary = Color(0xFFE11D48), onPrimary = Color.White, primaryContainer = Color(0xFF881337),
    secondary = Color(0xFF14B8A6), tertiary = Color(0xFFF59E0B),
    background = Color(0xFF020617), surface = Color(0xFF0F172A), surfaceVariant = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC), onSurface = Color(0xFFF8FAFC), onSurfaceVariant = Color(0xFF78716C),
    outline = Color(0xFF1E293B)
)

private val ModernDark = darkColorScheme(primary = Color(0xFFF7F7FA), onPrimary = Color.Black, secondary = Color(0xFFB0B0B7), tertiary = Color(0xFF0A84FF), background = Color.Black, surface = Color(0xFF1C1C1E), surfaceVariant = Color(0xFF2C2C2E), onBackground = Color(0xFFF7F7FA), onSurface = Color(0xFFF7F7FA), onSurfaceVariant = Color(0xFFB0B0B7), outline = Color(0xFF3A3A3C))
private val ModernLight = lightColorScheme(primary = Color(0xFF1C1C1E), onPrimary = Color.White, secondary = Color(0xFF77777E), tertiary = Color(0xFF0A84FF), background = Color(0xFFF2F2F7), surface = Color.White, surfaceVariant = Color(0xFFE5E5EA), onBackground = Color(0xFF111114), onSurface = Color(0xFF111114), onSurfaceVariant = Color(0xFF77777E), outline = Color(0xFFD1D1D6))
private val ClassicDark = darkColorScheme(primary = Color(0xFFC8F55A), onPrimary = Color(0xFF10150A), secondary = Color(0xFF5AF5C8), tertiary = Color(0xFFA78BFA), background = Color(0xFF08090D), surface = Color(0xFF12151E), surfaceVariant = Color(0xFF1C202E), onBackground = Color(0xFFF0F3FF), onSurface = Color(0xFFF0F3FF), onSurfaceVariant = Color(0xFFC0C7D6), outline = Color(0xFF343A4A))
private val ClassicLight = lightColorScheme(primary = Color(0xFF3F7D00), onPrimary = Color.White, secondary = Color(0xFF007A61), tertiary = Color(0xFF6848D9), background = Color(0xFFF0F4FF), surface = Color.White, surfaceVariant = Color(0xFFF8FAFF), onBackground = Color(0xFF0D1020), onSurface = Color(0xFF0D1020), onSurfaceVariant = Color(0xFF666D7C), outline = Color(0xFFD9DEEA))
private val WebDark = darkColorScheme(primary = Color(0xFFC8F55A), onPrimary = Color(0xFF08090D), secondary = Color(0xFF5AF5C8), tertiary = Color(0xFFA78BFA), background = Color(0xFF08090D), surface = Color(0xFF12151E), surfaceVariant = Color(0xFF1C202E), onBackground = Color(0xFFF0F3FF), onSurface = Color(0xFFF0F3FF), onSurfaceVariant = Color(0xFFC4CCBE), outline = Color(0xFF384A2C))
private val WebLight = lightColorScheme(primary = Color(0xFF3F7D00), onPrimary = Color.White, secondary = Color(0xFF007A61), tertiary = Color(0xFF6848D9), background = Color(0xFFF4F7EF), surface = Color.White, surfaceVariant = Color(0xFFF6F9F0), onBackground = Color(0xFF11170D), onSurface = Color(0xFF11170D), onSurfaceVariant = Color(0xFF63705D), outline = Color(0xFFC9D3C2))

val SpaceGrotesk = FontFamily(Font(R.font.space_grotesk_bold, FontWeight.Bold))
val IbmPlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium)
)
val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium)
)

private val TripTypography = Typography(
    headlineSmall = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = IbmPlexSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 1.sp)
)

@Composable fun TripNextTheme(theme: TripVisualTheme = TripVisualTheme.BOARDING, content: @Composable () -> Unit) {
    val colors = when (theme) {
        TripVisualTheme.BOARDING -> TripColors
        TripVisualTheme.MODERN_DARK -> ModernDark
        TripVisualTheme.MODERN_LIGHT -> ModernLight
        TripVisualTheme.CLASSIC_DARK -> ClassicDark
        TripVisualTheme.CLASSIC_LIGHT -> ClassicLight
        TripVisualTheme.WEB_DARK -> WebDark
        TripVisualTheme.WEB_LIGHT -> WebLight
    }
    MaterialTheme(colorScheme = colors, typography = TripTypography, content = content)
}
