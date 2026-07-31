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

val customThemeColor = mutableStateOf(
    AppConfigKey.run { mmkv.decodeInt(CUSTOM_THEME_COLOR, DEFAULT_THEME_COLOR) },
)

val predictiveBack = mutableStateOf(
    AppConfigKey.run { mmkv.decodeBool(PREDICTIVE_BACK, true) },
)

fun setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
    AppConfigKey.run { mmkv.encode(THEME_MODE, mode.name) }
}

fun setDynamicColor(enabled: Boolean) {
    dynamicColor.value = enabled
    AppConfigKey.run { mmkv.encode(DYNAMIC_COLOR, enabled) }
}

fun setCustomThemeColor(color: Int) {
    customThemeColor.value = color
    AppConfigKey.run { mmkv.encode(CUSTOM_THEME_COLOR, color) }
}

fun setPredictiveBack(enabled: Boolean) {
    predictiveBack.value = enabled
    AppConfigKey.run { mmkv.encode(PREDICTIVE_BACK, enabled) }
}

private fun decodeThemeMode(value: String?, legacyDarkMode: Boolean): ThemeMode =
    runCatching { ThemeMode.valueOf(value.orEmpty()) }
        .getOrDefault(if (legacyDarkMode) ThemeMode.DARK else ThemeMode.SYSTEM)

private const val DEFAULT_THEME_COLOR = 0xFF6750A4.toInt()
