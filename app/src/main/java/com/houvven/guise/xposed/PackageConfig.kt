package com.houvven.guise.xposed

import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.edit
import com.houvven.guise.ContextAmbient
import com.houvven.guise.xposed.config.ModuleConfig
import io.github.libxposed.service.XposedService

object PackageConfig {

    lateinit var current: ModuleConfig

    const val PREF_FILE_NAME = "XposedDeployInfo"

    val configurationRevision = mutableIntStateOf(0)

    val safePrefs: SharedPreferences
        get() = ContextAmbient.xposedService?.getRemotePreferences(PREF_FILE_NAME)
            ?: ContextAmbient.getSharedPreferences(PREF_FILE_NAME)

    internal fun migrateToRemotePreferences(service: XposedService) {
        val local = ContextAmbient.getSharedPreferences(PREF_FILE_NAME)
        if (local.all.isEmpty()) return
        val remote = service.getRemotePreferences(PREF_FILE_NAME)
        remote.edit {
            local.all.forEach { (key, value) ->
                if (remote.contains(key)) return@forEach
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }
    }

    fun notifyConfigurationsChanged() {
        configurationRevision.intValue++
    }

    fun doRefresh(packageName: String) {
        val prefs = ModernXposedPreferences.current
        current = prefs.getString(packageName, null)
            ?.let(ModuleConfig::fromJson)
            ?: ModuleConfig(enabled = false)
        current.packageName = packageName
    }
}

internal object ModernXposedPreferences {
    lateinit var current: SharedPreferences
}
