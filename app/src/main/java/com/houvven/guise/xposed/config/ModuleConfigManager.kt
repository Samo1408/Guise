package com.houvven.guise.xposed.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.core.content.edit
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.xposed.PackageConfig
import com.houvven.guise.xposed.ProcessControl
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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

    /** Saves spoofing parameters without changing the LSPosed scope. */
    fun save() {
        this.updateConfigFromState()
        persist()
    }

    /** The single entry point for changing both selection state and LSPosed scope. */
    fun setEnabled(enabled: Boolean, notifyOnScopeError: Boolean = true) {
        config.enabled = enabled
        persist()
        syncLsposedScope(enabled, notifyOnScopeError)
    }

    private fun persist() {
        safePrefs.edit(commit = true) { putString(config.packageName, config.toJson()) }
        LauncherState.setAppEnabled(config.packageName, config.enabled)
    }

    private fun syncLsposedScope(enable: Boolean, notifyOnError: Boolean) {
        val service = ContextAmbient.xposedService ?: run {
            if (notifyOnError) {
                reportScopeError(context.getString(R.string.xposed_service_not_connected))
            }
            return
        }
        if (!enable) {
            runCatching { service.removeScope(listOf(config.packageName)) }
                .onFailure {
                    if (notifyOnError) reportScopeError(it.message ?: it.toString())
                }
            return
        }
        service.requestScope(
            listOf(config.packageName),
            object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) = Unit
                override fun onScopeRequestFailed(message: String) {
                    if (notifyOnError) reportScopeError(message)
                }
            },
        )
    }

    private fun reportScopeError(message: String) {
        GlobalSnackbarHost.showOnErrorByDismissPrevious(
            context.getString(R.string.xposed_scope_sync_failed, message)
        )
    }

    suspend fun stopApp(): Result<Unit> {
        this.save()
        return stopWithFallback()
    }

    suspend fun restartApp(): Result<Unit> {
        this.save()
        return runCatching {
            stopWithFallback().getOrThrow()
            val intent = context.packageManager.getLaunchIntentForPackage(config.packageName)
                ?: error(context.getString(R.string.app_not_launchable))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }

    suspend fun stopIfHooked(): Result<Unit> =
        requestProcessExit().map { }

    private suspend fun stopWithFallback(): Result<Unit> {
        val rootResult = forceStopWithRoot()
        if (rootResult.isSuccess) return rootResult

        val xposedResult = requestProcessExit()
        if (xposedResult.getOrNull()?.let { it > 0 } == true) return Result.success(Unit)

        openApplicationDetails()
        return Result.failure(
            IllegalStateException(context.getString(R.string.manual_force_stop_required))
        )
    }

    private suspend fun requestProcessExit(): Result<Int> = runCatching {
        val service = ContextAmbient.xposedService ?: return@runCatching 0
        val targets = service.getRunningTargets().filter(::isTargetProcess)
        if (targets.isEmpty()) return@runCatching 0
        val targetPids = targets.mapTo(mutableSetOf()) { it.pid }
        val extras = Bundle().apply {
            putString(ProcessControl.EXTRA_COMMAND, ProcessControl.COMMAND_EXIT)
        }
        targets.forEach { target ->
            service.hotReloadModule(
                target,
                Bundle(extras),
                object : XposedService.HotReloadCallback {
                    override fun onHotReloadResult(
                        target: HookedTarget,
                        result: HotReloadResult,
                    ) = Unit
                },
            )
        }
        delay(PROCESS_EXIT_WAIT_MS)
        val remainingPids = service.getRunningTargets()
            .asSequence()
            .filter(::isTargetProcess)
            .map { it.pid }
            .filterTo(mutableSetOf()) { it in targetPids }
        if (remainingPids.isNotEmpty()) {
            error(context.getString(R.string.xposed_target_process_exit_failed))
        }
        targets.size
    }

    private suspend fun forceStopWithRoot(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(PACKAGE_NAME_PATTERN.matches(config.packageName)) {
                context.getString(R.string.invalid_package_name)
            }
            val command =
                "am force-stop --user current ${config.packageName}"
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(ROOT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy()
                error(context.getString(R.string.root_force_stop_timeout))
            }
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            check(process.exitValue() == 0) {
                output.ifBlank { context.getString(R.string.root_force_stop_failed) }
            }
        }
    }

    private fun isTargetProcess(target: HookedTarget): Boolean =
        target.processName == config.packageName ||
            target.processName.startsWith("${config.packageName}:")

    private fun openApplicationDetails() {
        context.startActivity(
            Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", config.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun updateConfigFromState() {
        val empty = ModuleConfig()
        val configFields = config.javaClass.declaredFields.toMutableList()
        val stateFields =
            state.javaClass.declaredFields.filter { it.type == MutableState::class.java }
        for (stateFiled in stateFields) {
            val configField = configFields.find { it.name == stateFiled.name } ?: continue
            stateFiled.isAccessible = true
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

        private const val PROCESS_EXIT_WAIT_MS = 800L
        private const val ROOT_COMMAND_TIMEOUT_SECONDS = 15L
        private val PACKAGE_NAME_PATTERN = Regex("[A-Za-z0-9._]+")

        fun of(config: ModuleConfig, state: ModuleConfigState) = ModuleConfigManager(config, state)

        fun of(config: ModuleConfig): ModuleConfigManager {
            val state = ModuleConfigState.of(config)
            return ModuleConfigManager(config, state)
        }

        fun empty() = of(ModuleConfig())
    }


}
