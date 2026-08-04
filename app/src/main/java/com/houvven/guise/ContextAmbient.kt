package com.houvven.guise

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.houvven.guise.log.RuntimeLogStore
import com.houvven.guise.xposed.PackageConfig
import com.houvven.guise.xposed.config.ModuleConfigManager
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class ContextAmbient : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var current: Context

        @Volatile
        var xposedService: XposedService? = null
            private set

        fun getSharedPreferences(
            name: String = BuildConfig.APPLICATION_ID,
            mode: Int = Context.MODE_PRIVATE,
        ) = current.getSharedPreferences(name, mode)
    }

    override fun onCreate() {
        super.onCreate()
        current = applicationContext
        RuntimeLogStore.initialize(this)
        MMKV.initialize(this, MMKVLogLevel.LevelNone)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        RuntimeLogStore.bind(service)
        PackageConfig.migrateToRemotePreferences(service)
        PackageConfig.notifyConfigurationsChanged()
        ModuleConfigManager.reconcileScope(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
            RuntimeLogStore.unbind()
            PackageConfig.notifyConfigurationsChanged()
        }
    }
}
