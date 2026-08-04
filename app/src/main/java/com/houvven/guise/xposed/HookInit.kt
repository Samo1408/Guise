package com.houvven.guise.xposed

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import com.houvven.guise.BuildConfig
import com.houvven.guise.xposed.hook.BatteryHook
import com.houvven.guise.xposed.hook.BuildConfigHook
import com.houvven.guise.xposed.hook.DisplayDensityHook
import com.houvven.guise.xposed.hook.LocalHook
import com.houvven.guise.xposed.hook.OsBuildHook
import com.houvven.guise.xposed.hook.ScreenshotsHook
import com.houvven.guise.xposed.hook.TimeZoneHook
import com.houvven.guise.xposed.hook.UniquelyIdHook
import com.houvven.guise.xposed.hook.location.CellLocationHook
import com.houvven.guise.xposed.hook.location.LocationHook
import com.houvven.guise.xposed.hook.netowork.NetworkHook
import com.houvven.guise.xposed.other.ApplicationListPass
import com.houvven.guise.xposed.other.BlankPass
import com.houvven.ktx_xposed.LoadPackageHookAdapter
import com.houvven.ktx_xposed.hook.afterHookedMethod
import com.houvven.ktx_xposed.hook.LoadPackageContext
import com.houvven.ktx_xposed.hook.ModernXposedRuntime
import com.houvven.ktx_xposed.logger.XposedLogger
import com.houvven.ktx_xposed.utils.runXposedCatching
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import kotlin.system.exitProcess

@Suppress("unused")
class HookInit : XposedModule() {

    private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
        log(Log.INFO, TAG, "Loaded in ${param.processName}; API $apiVersion")
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        if (scheduleProcessExit(param.extras)) return false
        return super.onHotReloading(param)
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        super.onHotReloaded(param)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName == BuildConfig.APPLICATION_ID) return

        ModernXposedRuntime.initialize(
            this,
            LoadPackageContext(param.packageName, processName, param.classLoader),
        )
        initializeLogDelivery()
        ModernXposedPreferences.current = getRemotePreferences(PackageConfig.PREF_FILE_NAME)

        XposedLogger.i("start onPackageReady: ${param.packageName}")
        PackageConfig.doRefresh(param.packageName)
        if (!PackageConfig.current.isEnable) {
            XposedLogger.i("${param.packageName} is not enabled, skip")
            XposedLogger.finishStartup()
            return
        }

        val hooks: List<Pair<String, LoadPackageHookAdapter>> = listOf(
            "Battery" to BatteryHook(),
            "Locale" to LocalHook(),
            "TimeZone" to TimeZoneHook(),
            "Location" to LocationHook(),
            "CellLocation" to CellLocationHook(),
            "Network" to NetworkHook(),
            "OSBuild" to OsBuildHook(),
            "DisplayDensity" to DisplayDensityHook(),
            "Screenshots" to ScreenshotsHook(),
            "UniqueId" to UniquelyIdHook(),
            "BlankPass" to BlankPass(),
            "ApplicationList" to ApplicationListPass(),
            "BuildConfig" to BuildConfigHook(),
        )
        hooks.forEach { (category, hook) ->
            val setupCompleted = runXposedCatching(category) {
                XposedLogger.withCategory(category) { hook.onHook() }
                true
            } == true
            if (setupCompleted) {
                XposedLogger.d("Hook setup completed", category)
            }
        }
        XposedLogger.finishStartup()
    }

    private fun initializeLogDelivery() {
        currentApplication()?.let(XposedLogger::attachContext)
        Application::class.java.afterHookedMethod("attach", Context::class.java) { param ->
            (param.args.firstOrNull() as? Context)?.let(XposedLogger::attachContext)
        }
        Activity::class.java.afterHookedMethod("onCreate", Bundle::class.java) { param ->
            (param.thisObject as? Activity)?.let(XposedLogger::attachContext)
        }
    }

    private fun currentApplication(): Application? = runCatching {
        val method = Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication")
        getInvoker(method).invoke(null) as? Application
    }.getOrNull()

    private fun scheduleProcessExit(extras: Bundle?): Boolean {
        if (!ProcessControl.isExitRequest(extras)) return false
        log(Log.INFO, TAG, "Process exit requested for $processName")
        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }, PROCESS_EXIT_DELAY_MS)
        return true
    }

    companion object {
        private const val TAG = "Guise"
        private const val PROCESS_EXIT_DELAY_MS = 150L
    }
}
