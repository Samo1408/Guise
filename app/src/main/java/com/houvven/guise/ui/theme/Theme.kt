package com.houvven.guise.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.dynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
    val wallpaperSeedColor = remember(wallpaperManager) { mutableStateOf<Int?>(null) }
    LaunchedEffect(wallpaperManager, dynamicColor.value) {
        if (dynamicColor.value) {
            wallpaperSeedColor.value = withContext(Dispatchers.IO) {
                wallpaperManager.systemSeedColor()
            }
        }
    }
    DisposableEffect(wallpaperManager, dynamicColor.value) {
        if (!dynamicColor.value) return@DisposableEffect onDispose { }
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                wallpaperSeedColor.value = colors?.primaryColor?.toArgb()
            }
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { wallpaperManager.removeOnColorsChangedListener(listener) }
    }

    val requestedSeed = if (dynamicColor.value) {
        wallpaperSeedColor.value
    } else {
        customThemeColor.intValue
    }
    val generatedColorScheme by produceState<ColorScheme?>(
        initialValue = null,
        requestedSeed,
        darkMode,
    ) {
        value = requestedSeed?.let { seed ->
            withContext(Dispatchers.Default) {
                dynamicColorScheme(
                    seedColor = Color(seed),
                    isDark = darkMode,
                    isAmoled = false,
                )
            }
        }
    }
    val fallbackColorScheme = remember(context, darkMode, dynamicColor.value) {
        if (dynamicColor.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (darkMode) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    }
    val colorScheme = generatedColorScheme ?: fallbackColorScheme
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
