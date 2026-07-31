package com.houvven.guise.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.houvven.guise.constant.AppConfigKey

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private const val LEGACY_ALWAYS_DARK_MODE = "always.dark.mode"

val themeMode = mutableStateOf(
    AppConfigKey.run {
        decodeThemeMode(
            mmkv.decodeString(THEME_MODE),
            legacyDarkMode = mmkv.decodeBool(LEGACY_ALWAYS_DARK_MODE, false),
        )
    },
)

val dynamicColor = mutableStateOf(
    AppConfigKey.run { mmkv.decodeBool(DYNAMIC_COLOR, true) },
)

fun setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
    AppConfigKey.run { mmkv.encode(THEME_MODE, mode.name) }
}

fun setDynamicColor(enabled: Boolean) {
    dynamicColor.value = enabled
    AppConfigKey.run { mmkv.encode(DYNAMIC_COLOR, enabled) }
}

private fun decodeThemeMode(value: String?, legacyDarkMode: Boolean): ThemeMode =
    runCatching { ThemeMode.valueOf(value.orEmpty()) }
        .getOrDefault(if (legacyDarkMode) ThemeMode.DARK else ThemeMode.SYSTEM)
