package com.houvven.guise.xposed

import android.util.Log
import com.houvven.guise.BuildConfig
import com.houvven.guise.xposed.hook.BatteryHook
import com.houvven.guise.xposed.hook.BuildConfigHook
import com.houvven.guise.xposed.hook.LocalHook
import com.houvven.guise.xposed.hook.OsBuildHook
import com.houvven.guise.xposed.hook.ScreenshotsHook
import com.houvven.guise.xposed.hook.UniquelyIdHook
import com.houvven.guise.xposed.hook.location.CellLocationHook
import com.houvven.guise.xposed.hook.location.LocationHook
import com.houvven.guise.xposed.hook.netowork.NetworkHook
import com.houvven.guise.xposed.other.BlankPass
import com.houvven.guise.xposed.other.HookSuccessHint
import com.houvven.ktx_xposed.LoadPackageHookAdapter
import com.houvven.ktx_xposed.hook.LoadPackageContext
import com.houvven.ktx_xposed.hook.ModernXposedRuntime
import com.houvven.ktx_xposed.logger.XposedLogger
import com.houvven.ktx_xposed.utils.runXposedCatching
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

@Suppress("unused")
class HookInit : XposedModule() {

    private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
        log(Log.INFO, TAG, "Loaded in ${param.processName}; API $apiVersion")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName == BuildConfig.APPLICATION_ID) return

        ModernXposedRuntime.initialize(
            this,
            LoadPackageContext(param.packageName, processName, param.classLoader),
        )
        ModernXposedPreferences.current = getRemotePreferences(PackageConfig.PREF_FILE_NAME)

        XposedLogger.i("start onPackageReady: ${param.packageName}")
        PackageConfig.doRefresh(param.packageName)
        if (!PackageConfig.current.isEnable) {
            XposedLogger.i("${param.packageName} is not enabled, skip")
            return
        }

        listOf(
            HookSuccessHint(),
            BatteryHook(),
            LocalHook(),
            LocationHook(),
            CellLocationHook(),
            NetworkHook(),
            OsBuildHook(),
            ScreenshotsHook(),
            UniquelyIdHook(),
            BlankPass(),
            BuildConfigHook(),
        ).forEach { hook: LoadPackageHookAdapter ->
            runXposedCatching { hook.onHook() }
        }
        XposedLogger.doHookModuleLog()
    }

    companion object {
        private const val TAG = "Guise"
    }
}
