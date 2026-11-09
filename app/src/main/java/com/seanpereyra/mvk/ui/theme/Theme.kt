package com.seanpereyra.mvk.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.seanpereyra.mvk.R
import com.seanpereyra.mvk.data.Accent
import com.seanpereyra.mvk.data.AppSettings
import com.seanpereyra.mvk.data.ThemeMode

// ---------- Fonts ----------
val Display = FontFamily(
    Font(R.font.inter_display_black, FontWeight.Black),
    Font(R.font.inter_display_extrabold, FontWeight.ExtraBold),
)
val Body = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
)
val Mono = FontFamily(Font(R.font.dejavu_mono_bold, FontWeight.Bold))

object MvkType {
    val logo = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-0.02).em)
    val title = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-0.03).em, lineHeight = 44.sp)
    val button = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 0.02.em)
    val section = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.14.em)
    val body = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val small = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val code = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold)
}

// ---------- Colors ----------
@Immutable
data class MvkColors(
    val bg: Color,
    val surface: Color,
    val nav: Color,
    val text: Color,
    val textDim: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val ticket: Color = Color.White,
    val ink: Color = Color(0xFF111111),
    val isDark: Boolean,
)

val LocalMvk = staticCompositionLocalOf {
    MvkColors(Color(0xFF1A1A1A), Color(0xFF262626), Color(0xFF404040), Color.White, Color(0xFFA0A0A0),
        Color(0xFF3A3A3A), Color.White, Color(0xFF111111), isDark = true)
}

object Mvk {
    val colors: MvkColors @Composable get() = LocalMvk.current
}

@Composable
fun MvkTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val mode = if (settings.theme == ThemeMode.SYSTEM) (if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT) else settings.theme

    val base = when (mode) {
        ThemeMode.LIGHT -> MvkColors(
            bg = Color(0xFFF2F1EC), surface = Color(0xFFFFFFFF), nav = Color(0xFF1A1A1A),
            text = Color(0xFF141414), textDim = Color(0xFF6B6B66), line = Color(0xFFDAD9D2),
            accent = Color.Black, onAccent = Color.White, isDark = false,
        )
        ThemeMode.AMOLED -> MvkColors(
            bg = Color.Black, surface = Color(0xFF121212), nav = Color(0xFF1E1E1E),
            text = Color.White, textDim = Color(0xFF8E8E8E), line = Color(0xFF262626),
            accent = Color.White, onAccent = Color.Black, isDark = true,
        )
        else -> MvkColors(
            bg = Color(0xFF1A1A1A), surface = Color(0xFF262626), nav = Color(0xFF404040),
            text = Color.White, textDim = Color(0xFFA3A3A3), line = Color(0xFF383838),
            accent = Color.White, onAccent = Color(0xFF111111), isDark = true,
        )
    }
    val colors = if (settings.accent == Accent.MONO) base else {
        val a = Color(settings.accent.argb)
        base.copy(accent = a, onAccent = if (a.luminance() > 0.45f) Color(0xFF111111) else Color.White)
    }

    val scheme = if (colors.isDark) darkColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.text,
        surface = colors.surface, onSurface = colors.text, surfaceContainerLow = colors.surface,
        surfaceContainerHigh = colors.surface, onSurfaceVariant = colors.textDim, outline = colors.line,
    ) else lightColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.text,
        surface = colors.surface, onSurface = colors.text, surfaceContainerLow = colors.surface,
        surfaceContainerHigh = colors.surface, onSurfaceVariant = colors.textDim, outline = colors.line,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.bg.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.bg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !colors.isDark
                isAppearanceLightNavigationBars = !colors.isDark
            }
        }
    }

    val t = Typography()
    val typography = Typography(
        bodyLarge = t.bodyLarge.copy(fontFamily = Body),
        bodyMedium = t.bodyMedium.copy(fontFamily = Body),
        bodySmall = t.bodySmall.copy(fontFamily = Body),
        titleLarge = t.titleLarge.copy(fontFamily = Body, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = Body, fontWeight = FontWeight.Bold),
        labelLarge = t.labelLarge.copy(fontFamily = Body, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.Black),
    )

    CompositionLocalProvider(LocalMvk provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
