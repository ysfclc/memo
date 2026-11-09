package app.memo.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Memo's visual foundation.
 *
 * Keep existing token names stable while new surfaces migrate. Feature screens should
 * consume MaterialTheme and these semantic tokens instead of defining local palettes.
 */
object MemoTokens {
    object Brand {
        // Signal Mint: a quiet, recognisable accent for intent, focus and active work.
        val signal = Color(0xFF087A63)
        val signalBright = Color(0xFF72E2BE)
        val midnight = Color(0xFF0B1210)
        val graphite = Color(0xFF17211E)
        val paper = Color(0xFFF4F6F2)
        val ink = Color(0xFF17201D)
        val violet = Color(0xFF7775C8) // Reserved for AI/council context, never general decoration.
    }

    object Spacing {
        val xxxs = 2.dp
        val xxs = 4.dp
        val xs = 8.dp
        val sm = 12.dp
        val md = 16.dp
        val lg = 20.dp
        val xl = 24.dp
        val xxl = 32.dp
        val xxxl = 48.dp
        val pageMargin = 20.dp
        val minimumTouch = 48.dp
    }

    object Radius {
        val extraSmall = 6.dp
        val small = 10.dp
        val medium = 14.dp
        val large = 20.dp
        val extraLarge = 26.dp
        val pill = 100.dp
    }

    object Elevation {
        val flat = 0.dp
        val raised = 2.dp
        val floating = 6.dp
        val modal = 12.dp
    }

    object Motion {
        const val instant = 90
        const val quick = 150
        const val standard = 220
        const val expressive = 320
    }

    object SemanticLight {
        val success = Color(0xFF176B52)
        val onSuccess = Color(0xFFFFFFFF)
        val successContainer = Color(0xFFD8F2E6)
        val onSuccessContainer = Color(0xFF082D20)
        val warning = Color(0xFF805000)
        val onWarning = Color(0xFFFFFFFF)
        val warningContainer = Color(0xFFFFE8BD)
        val onWarningContainer = Color(0xFF352000)
        val info = Color(0xFF315D9B)
        val onInfo = Color(0xFFFFFFFF)
        val infoContainer = Color(0xFFE0EBFF)
        val onInfoContainer = Color(0xFF142D52)
    }

    object SemanticDark {
        val success = Color(0xFF83DDB9)
        val onSuccess = Color(0xFF063B2B)
        val successContainer = Color(0xFF14563F)
        val onSuccessContainer = Color(0xFFB5F4D6)
        val warning = Color(0xFFFFC66E)
        val onWarning = Color(0xFF462B00)
        val warningContainer = Color(0xFF684400)
        val onWarningContainer = Color(0xFFFFE7B7)
        val info = Color(0xFFA8C8FF)
        val onInfo = Color(0xFF17345D)
        val infoContainer = Color(0xFF254A79)
        val onInfoContainer = Color(0xFFDCE8FF)
    }

    object Light {
        val primary = Color(0xFF087A63)
        val onPrimary = Color(0xFFFFFFFF)
        val primaryContainer = Color(0xFFD4F1E6)
        val onPrimaryContainer = Color(0xFF002F24)
        val secondary = Color(0xFF52635D)
        val onSecondary = Color(0xFFFFFFFF)
        val secondaryContainer = Color(0xFFDCE8E1)
        val onSecondaryContainer = Color(0xFF17231E)
        val tertiary = Color(0xFF6262A7)
        val onTertiary = Color(0xFFFFFFFF)
        val tertiaryContainer = Color(0xFFE8E5FF)
        val onTertiaryContainer = Color(0xFF24234E)
        val background = Color(0xFFF4F6F2)
        val onBackground = Color(0xFF17201D)
        val surface = Color(0xFFFCFDFC)
        val onSurface = Color(0xFF17201D)
        val surfaceVariant = Color(0xFFE8EDE8)
        val onSurfaceVariant = Color(0xFF46534D)
        val outline = Color(0xFF77847D)
        val outlineVariant = Color(0xFFD1DAD3)
        val error = Color(0xFFB3261E)
        val onError = Color(0xFFFFFFFF)
        val errorContainer = Color(0xFFF9DEDC)
        val onErrorContainer = Color(0xFF410E0B)
    }

    object Dark {
        val primary = Color(0xFF72E2BE)
        val onPrimary = Color(0xFF00382C)
        val primaryContainer = Color(0xFF075B49)
        val onPrimaryContainer = Color(0xFFB7F5DD)
        val secondary = Color(0xFFB8CBC1)
        val onSecondary = Color(0xFF24352E)
        val secondaryContainer = Color(0xFF34473F)
        val onSecondaryContainer = Color(0xFFD4E7DC)
        val tertiary = Color(0xFFBDBBFF)
        val onTertiary = Color(0xFF29285F)
        val tertiaryContainer = Color(0xFF45447E)
        val onTertiaryContainer = Color(0xFFE4E1FF)
        val background = Color(0xFF0B1210)
        val onBackground = Color(0xFFE5EEE9)
        val surface = Color(0xFF121B18)
        val onSurface = Color(0xFFE5EEE9)
        val surfaceVariant = Color(0xFF202C27)
        val onSurfaceVariant = Color(0xFFB6C5BD)
        val outline = Color(0xFF81938A)
        val outlineVariant = Color(0xFF35443D)
        val error = Color(0xFFFFB4AB)
        val onError = Color(0xFF690005)
        val errorContainer = Color(0xFF93000A)
        val onErrorContainer = Color(0xFFFFDAD6)
    }

    fun colors(dark: Boolean): ColorScheme = if (dark) darkColorScheme(
        primary = Dark.primary, onPrimary = Dark.onPrimary, primaryContainer = Dark.primaryContainer,
        onPrimaryContainer = Dark.onPrimaryContainer, secondary = Dark.secondary, onSecondary = Dark.onSecondary,
        secondaryContainer = Dark.secondaryContainer, onSecondaryContainer = Dark.onSecondaryContainer,
        tertiary = Dark.tertiary, onTertiary = Dark.onTertiary, tertiaryContainer = Dark.tertiaryContainer,
        onTertiaryContainer = Dark.onTertiaryContainer, background = Dark.background, onBackground = Dark.onBackground,
        surface = Dark.surface, onSurface = Dark.onSurface, surfaceVariant = Dark.surfaceVariant,
        onSurfaceVariant = Dark.onSurfaceVariant, outline = Dark.outline, outlineVariant = Dark.outlineVariant,
        error = Dark.error, onError = Dark.onError, errorContainer = Dark.errorContainer,
        onErrorContainer = Dark.onErrorContainer
    ) else lightColorScheme(
        primary = Light.primary, onPrimary = Light.onPrimary, primaryContainer = Light.primaryContainer,
        onPrimaryContainer = Light.onPrimaryContainer, secondary = Light.secondary, onSecondary = Light.onSecondary,
        secondaryContainer = Light.secondaryContainer, onSecondaryContainer = Light.onSecondaryContainer,
        tertiary = Light.tertiary, onTertiary = Light.onTertiary, tertiaryContainer = Light.tertiaryContainer,
        onTertiaryContainer = Light.onTertiaryContainer, background = Light.background, onBackground = Light.onBackground,
        surface = Light.surface, onSurface = Light.onSurface, surfaceVariant = Light.surfaceVariant,
        onSurfaceVariant = Light.onSurfaceVariant, outline = Light.outline, outlineVariant = Light.outlineVariant,
        error = Light.error, onError = Light.onError, errorContainer = Light.errorContainer,
        onErrorContainer = Light.onErrorContainer
    )

    val typography = Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.7).sp),
        displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.25).sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.1.sp)
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(26.dp)
    )
}

@Composable
fun MemoTheme(themeMode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = MemoTokens.colors(dark), typography = MemoTokens.typography, shapes = MemoTokens.shapes, content = content)
}
