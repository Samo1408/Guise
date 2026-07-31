package com.houvven.guise.xposed.hook

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookAllMethods
import com.houvven.ktx_xposed.hook.findClassIfExists
import com.houvven.ktx_xposed.hook.lppram

class BuildConfigHook : LoadPackageHandler {

    override fun onHook() {
        val packageManagerClass = findClassIfExists("android.app.ApplicationPackageManager")
            ?: PackageManager::class.java
        packageManagerClass.run {
            afterHookAllMethods("getPackageInfo") {
                val requestedPackage = when (val argument = it.args.firstOrNull()) {
                    is String -> argument
                    is VersionedPackage -> argument.packageName
                    else -> null
                }
                if (requestedPackage == lppram.packageName) {
                    (it.result as? PackageInfo)?.apply {
                        if (config.versionName.isNotBlank()) versionName = config.versionName
                        if (config.versionCode != -1) longVersionCode = config.versionCode.toLong()
                    }
                }
            }
        }
    }
}
