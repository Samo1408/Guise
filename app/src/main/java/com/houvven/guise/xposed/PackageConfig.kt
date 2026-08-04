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
    private const val MIGRATION_STATE_FILE = "xposed_config_storage_v2"
    private const val LOCAL_SOURCE_INITIALIZED = "local_source_initialized"

    val configurationRevision = mutableIntStateOf(0)

    val safePrefs: SharedPreferences
        get() = ContextAmbient.getSharedPreferences(PREF_FILE_NAME)

    internal fun migrateToRemotePreferences(service: XposedService) {
        val local = safePrefs
        val remote = service.getRemotePreferences(PREF_FILE_NAME)
        val migrationState = ContextAmbient.getSharedPreferences(MIGRATION_STATE_FILE)
        if (!migrationState.getBoolean(LOCAL_SOURCE_INITIALIZED, false)) {
            if (remote.all.isNotEmpty()) {
                replacePreferences(local, remote.all)
            } else {
                replacePreferences(remote, local.all)
            }
            migrationState.edit(commit = true) {
                putBoolean(LOCAL_SOURCE_INITIALIZED, true)
            }
            return
        }
        replacePreferences(remote, local.all)
    }

    fun notifyConfigurationsChanged() {
        ContextAmbient.xposedService?.let { service ->
            replacePreferences(
                service.getRemotePreferences(PREF_FILE_NAME),
                safePrefs.all,
            )
        }
        configurationRevision.intValue++
    }

    private fun replacePreferences(
        destination: SharedPreferences,
        values: Map<String, *>,
    ) {
        destination.edit(commit = true) {
            clear()
            values.forEach { (key, value) ->
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
