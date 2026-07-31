package com.houvven.guise.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.dynamicColorScheme


@Composable
fun GuiseTheme(
    content: @Composable () -> Unit,
) {
    val systemDark =
        (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val darkMode = themeMode.value == ThemeMode.DARK ||
        (themeMode.value == ThemeMode.SYSTEM && systemDark)
    val context = LocalContext.current
    val wallpaperManager = remember(context) { WallpaperManager.getInstance(context) }
    val wallpaperSeedColor = remember(wallpaperManager) {
        mutableStateOf(wallpaperManager.systemSeedColor())
    }
    DisposableEffect(wallpaperManager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                wallpaperSeedColor.value = colors?.primaryColor?.toArgb()
            }
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { wallpaperManager.removeOnColorsChangedListener(listener) }
    }
    val colorScheme = when {
        dynamicColor.value && wallpaperSeedColor.value != null -> dynamicColorScheme(
            seedColor = Color(wallpaperSeedColor.value!!),
            isDark = darkMode,
            isAmoled = false,
        )
        dynamicColor.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> dynamicColorScheme(
            seedColor = Color(customThemeColor.value),
            isDark = darkMode,
            isAmoled = false,
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun WallpaperManager.systemSeedColor(): Int? =
    runCatching { getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() }
        .getOrNull()
