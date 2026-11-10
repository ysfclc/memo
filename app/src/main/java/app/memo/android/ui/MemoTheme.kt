package app.memo.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape

/** Single design-token source. Components consume MaterialTheme rather than ad-hoc styling. */
object MemoTokens {
    object Spacing {
        val xxs = 4.dp
        val xs = 8.dp
        val sm = 12.dp
        val md = 16.dp
        val lg = 20.dp
        val xl = 24.dp
        val xxl = 32.dp
        val pageMargin = 20.dp
        val minimumTouch = 48.dp
    }

    object Radius {
        val small = 10.dp
        val medium = 16.dp
        val large = 24.dp
        val pill = 100.dp
    }

    object SemanticLight {
        val success = Color(0xFF006B5F)
        val onSuccess = Color(0xFFFFFFFF)
        val successContainer = Color(0xFFD1F5EC)
        val onSuccessContainer = Color(0xFF00201A)
        val warning = Color(0xFF7A4D00)
        val onWarning = Color(0xFFFFFFFF)
        val warningContainer = Color(0xFFFFDEA6)
        val onWarningContainer = Color(0xFF291800)
        val info = Color(0xFF215AAB)
        val onInfo = Color(0xFFFFFFFF)
        val infoContainer = Color(0xFFD9E2FF)
        val onInfoContainer = Color(0xFF001A41)
    }

    object SemanticDark {
        val success = Color(0xFF82D9CA)
        val onSuccess = Color(0xFF003731)
        val successContainer = Color(0xFF005047)
        val onSuccessContainer = Color(0xFF9AF2E1)
        val warning = Color(0xFFFFB95C)
        val onWarning = Color(0xFF4A2800)
        val warningContainer = Color(0xFF664000)
        val onWarningContainer = Color(0xFFFFDEA6)
        val info = Color(0xFFA7C8FF)
        val onInfo = Color(0xFF00305D)
        val infoContainer = Color(0xFF194777)
        val onInfoContainer = Color(0xFFD9E2FF)
    }

    object Light {
        val primary = Color(0xFF373CA6)
        val onPrimary = Color(0xFFFFFFFF)
        val primaryContainer = Color(0xFFE3E5FF)
        val onPrimaryContainer = Color(0xFF10145C)
        val secondary = Color(0xFF53617A)
        val onSecondary = Color(0xFFFFFFFF)
        val secondaryContainer = Color(0xFFE3E8F5)
        val onSecondaryContainer = Color(0xFF171F33)
        val tertiary = Color(0xFF006B5F)
        val onTertiary = Color(0xFFFFFFFF)
        val tertiaryContainer = Color(0xFF9AF2E1)
        val onTertiaryContainer = Color(0xFF00201A)
        val background = Color(0xFFF6F7FB)
        val onBackground = Color(0xFF171A28)
        val surface = Color(0xFFFFFFFF)
        val onSurface = Color(0xFF171A28)
        val surfaceVariant = Color(0xFFE8EAF3)
        val onSurfaceVariant = Color(0xFF44485A)
        val outline = Color(0xFF73788B)
        val outlineVariant = Color(0xFFD1D4E1)
        val error = Color(0xFFBA1A1A)
        val onError = Color(0xFFFFFFFF)
        val errorContainer = Color(0xFFFFDAD6)
        val onErrorContainer = Color(0xFF410002)
    }

    object Dark {
        val primary = Color(0xFFB9BFFF)
        val onPrimary = Color(0xFF171A5E)
        val primaryContainer = Color(0xFF252A76)
        val onPrimaryContainer = Color(0xFFE2E4FF)
        val secondary = Color(0xFFC0C7DF)
        val onSecondary = Color(0xFF283146)
        val secondaryContainer = Color(0xFF3E475F)
        val onSecondaryContainer = Color(0xFFDEE4FC)
        val tertiary = Color(0xFF82D9CA)
        val onTertiary = Color(0xFF003731)
        val tertiaryContainer = Color(0xFF005047)
        val onTertiaryContainer = Color(0xFF9AF2E1)
        val background = Color(0xFF11131D)
        val onBackground = Color(0xFFE6E7F2)
        val surface = Color(0xFF191C29)
        val onSurface = Color(0xFFE6E7F2)
        val surfaceVariant = Color(0xFF292D3D)
        val onSurfaceVariant = Color(0xFFC2C5D7)
        val outline = Color(0xFF8C91A6)
        val outlineVariant = Color(0xFF414659)
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
        displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp)
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
