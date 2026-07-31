package com.houvven.guise.xposed.config

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.MutableState
import androidx.core.content.edit
import androidx.core.net.toUri
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.xposed.PackageConfig
import com.houvven.guise.util.android.ShellExecutor
import io.github.libxposed.service.XposedService

class ModuleConfigManager
private constructor(
    val config: ModuleConfig,
    val state: ModuleConfigState,
) {

    private val safePrefs
        get() = PackageConfig.safePrefs

    private val context = ContextAmbient.current

    fun clear() {
        state.clear()
    }

    fun save() {
        this.updateConfigFromState()
        val json = config.toJson()
        val enable = config.isEnable
        LauncherState.apps.value.find { it.packageName == config.packageName }?.isEnable = enable
        if (enable) {
            safePrefs.edit { putString(config.packageName, json) }
        } else {
            safePrefs.edit(commit = true) { remove(config.packageName) }
        }
        syncLsposedScope(enable)
    }

    private fun syncLsposedScope(enable: Boolean) {
        val service = ContextAmbient.xposedService ?: run {
            reportScopeError(context.getString(R.string.xposed_service_not_connected))
            return
        }
        if (!enable) {
            runCatching { service.removeScope(listOf(config.packageName)) }
                .onFailure { reportScopeError(it.message ?: it.toString()) }
            return
        }
        service.requestScope(
            listOf(config.packageName),
            object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) = Unit
                override fun onScopeRequestFailed(message: String) = reportScopeError(message)
            },
        )
    }

    private fun reportScopeError(message: String) {
        GlobalSnackbarHost.showOnErrorByDismissPrevious(
            context.getString(R.string.xposed_scope_sync_failed, message)
        )
    }

    fun stopApp(): Boolean {
        this.save()
        var isUseRootSucceed = true
        val result = ShellExecutor.execute("am force-stop ${config.packageName}", asRoot = true)
        result.onFailure {
            isUseRootSucceed = false
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = "package:${config.packageName}".toUri()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        return isUseRootSucceed
    }

    fun restartApp(): Result<Unit> {
        this.save()
        return runCatching {
            ShellExecutor.execute("am force-stop ${config.packageName}", asRoot = true).onFailure {
                throw RuntimeException(context.getString(R.string.no_root_prompt))
            }
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(config.packageName)!!
            context.startActivity(intent)
        }
    }

    fun updateConfigFromState() {
        val empty = ModuleConfig()
        val configFields = config.javaClass.declaredFields.toMutableList()
        val stateFields =
            state.javaClass.declaredFields.filter { it.type == MutableState::class.java }
        for (stateFiled in stateFields) {
            val configField = configFields.find { it.name == stateFiled.name } ?: continue
            val value = (stateFiled.get(state) as MutableState<*>).value
            configField.isAccessible = true
            // if (configField.get(empty) == value) continue

            if (configField.type == Boolean::class.java) {
                configField.setBoolean(config, value as Boolean)
                continue
            } else if (configField.type == String::class.java) {
                configField.set(config, value as String)
                continue
            }

            value as String
            when (configField.type) {
                Int::class.java -> (value.toIntOrNull() ?: configField.getInt(empty))
                    .let { configField.setInt(config, it) }

                Long::class.java -> (value.toLongOrNull() ?: configField.getLong(empty))
                    .let { configField.setLong(config, it) }

                Short::class.java -> (value.toShortOrNull() ?: configField.getShort(empty))
                    .let { configField.setShort(config, it) }

                Byte::class.java -> (value.toByteOrNull() ?: configField.getByte(empty))
                    .let { configField.setByte(config, it) }

                Double::class.java -> (value.toDoubleOrNull() ?: configField.getDouble(empty))
                    .let { configField.setDouble(config, it) }

                Float::class.java -> (value.toFloatOrNull() ?: configField.getFloat(empty))
                    .let { configField.setFloat(config, it) }

                Char::class.java -> (value.singleOrNull() ?: configField.getChar(empty))
                    .let { configField.setChar(config, it) }

                else -> Unit
            }
        }
    }

    companion object {

        fun of(config: ModuleConfig, state: ModuleConfigState) = ModuleConfigManager(config, state)

        fun of(config: ModuleConfig): ModuleConfigManager {
            val state = ModuleConfigState.of(config)
            return ModuleConfigManager(config, state)
        }

        fun empty() = of(ModuleConfig())
    }


}
