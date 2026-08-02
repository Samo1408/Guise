@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.other

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.VersionedPackage
import android.os.Process
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookAllMethods
import com.houvven.ktx_xposed.hook.findClassIfExists
import com.houvven.ktx_xposed.hook.lppram
import java.lang.reflect.Array as ReflectArray
import java.util.concurrent.ConcurrentHashMap

/**
 * Filters package discovery inside the hooked app process only.
 *
 * Unlike framework-wide package filters, this deliberately does not hook system_server. Public
 * PackageManager calls and direct IPackageManager proxy calls made from the target process are
 * filtered, while the target itself and packages marked as system apps remain visible.
 */
class ApplicationListPass : LoadPackageHandler {

    private val visiblePackages = ConcurrentHashMap.newKeySet<String>()

    override fun onHook() {
        if (!config.passApplications) return

        visiblePackages += lppram.packageName
        visiblePackages += ANDROID_PACKAGE

        findClassIfExists(APPLICATION_PACKAGE_MANAGER)?.let { hookPackageManager(it, true) }
        findClassIfExists(IPACKAGE_MANAGER_PROXY)?.let { hookPackageManager(it, false) }
    }

    private fun hookPackageManager(clazz: Class<*>, publicApiLayer: Boolean) {
        LIST_METHODS.forEach { methodName ->
            clazz.afterHookAllMethods(methodName) { param ->
                param.result = filterCollectionResult(param.result)
            }
        }

        PACKAGE_NAME_ARRAY_METHODS.forEach { methodName ->
            clazz.afterHookAllMethods(methodName) { param ->
                val uid = param.args.firstOrNull() as? Int
                if (uid != null && uid < Process.FIRST_APPLICATION_UID) return@afterHookAllMethods
                param.result = filterPackageNameArray(param.result)
            }
        }

        INFO_METHODS.forEach { methodName ->
            clazz.afterHookAllMethods(methodName) { param ->
                val result = param.result ?: return@afterHookAllMethods
                if (shouldExpose(result)) return@afterHookAllMethods

                if (publicApiLayer && methodName in NAME_NOT_FOUND_METHODS) {
                    throw PackageManager.NameNotFoundException(requestedPackageName(param.args))
                }
                param.result = null
            }
        }
    }

    private fun filterCollectionResult(result: Any?): Any? = when {
        result == null -> null
        result is List<*> -> result.filter(::shouldExpose)
        result.javaClass.isArray -> filterArray(result)
        isParceledListSlice(result) -> filterParceledListSlice(result)
        else -> result
    }

    private fun filterArray(result: Any): Any {
        val length = ReflectArray.getLength(result)
        val filtered = buildList {
            repeat(length) { index ->
                ReflectArray.get(result, index)?.let { item ->
                    if (shouldExpose(item)) add(item)
                }
            }
        }
        val componentType = result.javaClass.componentType ?: return result
        val output = ReflectArray.newInstance(componentType, filtered.size)
        filtered.forEachIndexed { index, item -> ReflectArray.set(output, index, item) }
        return output
    }

    private fun filterPackageNameArray(result: Any?): Any? {
        if (result !is Array<*>) return result
        return result.filterIsInstance<String>()
            .filter(::isKnownVisiblePackage)
            .toTypedArray()
    }

    private fun filterParceledListSlice(result: Any): Any {
        val list = runCatching {
            result.javaClass.methods.firstOrNull {
                it.name == "getList" && it.parameterCount == 0
            }?.invoke(result) as? List<*>
        }.getOrNull() ?: return result

        val filtered = list.filter(::shouldExpose)
        if (filtered.size == list.size) return result

        val mutated = runCatching {
            @Suppress("UNCHECKED_CAST")
            (list as MutableList<Any?>).apply {
                clear()
                addAll(filtered)
            }
        }.isSuccess
        if (mutated) return result

        return runCatching {
            result.javaClass.getDeclaredConstructor(List::class.java).apply {
                isAccessible = true
            }.newInstance(filtered)
        }.getOrDefault(result)
    }

    private fun shouldExpose(value: Any?): Boolean {
        val packageName: String
        val applicationInfo: ApplicationInfo?
        when (value) {
            is PackageInfo -> {
                packageName = value.packageName
                applicationInfo = value.applicationInfo
                if (value.isApex) return rememberVisible(packageName)
            }

            is ApplicationInfo -> {
                packageName = value.packageName
                applicationInfo = value
            }

            is ResolveInfo -> {
                val component = value.activityInfo ?: value.serviceInfo ?: value.providerInfo
                packageName = component?.packageName ?: value.resolvePackageName.orEmpty()
                applicationInfo = component?.applicationInfo
            }

            is ComponentInfo -> {
                packageName = value.packageName
                applicationInfo = value.applicationInfo
            }

            is String -> return isKnownVisiblePackage(value)
            else -> return false
        }

        if (isKnownVisiblePackage(packageName)) return true
        if (applicationInfo?.isSystemPackage() == true) return rememberVisible(packageName)
        return false
    }

    private fun ApplicationInfo.isSystemPackage(): Boolean =
        flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    private fun rememberVisible(packageName: String): Boolean {
        if (packageName.isNotBlank()) visiblePackages += packageName
        return true
    }

    private fun isKnownVisiblePackage(packageName: String): Boolean =
        packageName.isNotBlank() && packageName in visiblePackages

    private fun requestedPackageName(args: Array<Any?>): String = when (val value = args.firstOrNull()) {
        is String -> value
        is VersionedPackage -> value.packageName
        is ComponentName -> value.packageName
        else -> ""
    }

    private fun isParceledListSlice(value: Any): Boolean =
        value.javaClass.name == PARCELED_LIST_SLICE ||
            value.javaClass.superclass?.name == BASE_PARCELED_LIST_SLICE

    companion object {
        private const val ANDROID_PACKAGE = "android"
        private const val APPLICATION_PACKAGE_MANAGER = "android.app.ApplicationPackageManager"
        private const val IPACKAGE_MANAGER_PROXY = "android.content.pm.IPackageManager\$Stub\$Proxy"
        private const val PARCELED_LIST_SLICE = "android.content.pm.ParceledListSlice"
        private const val BASE_PARCELED_LIST_SLICE = "android.content.pm.BaseParceledListSlice"

        private val LIST_METHODS = setOf(
            "getInstalledApplications",
            "getInstalledPackages",
            "getPackagesHoldingPermissions",
            "queryBroadcastReceivers",
            "queryContentProviders",
            "queryIntentActivities",
            "queryIntentActivityOptions",
            "queryIntentContentProviders",
            "queryIntentServices",
        )

        private val PACKAGE_NAME_ARRAY_METHODS = setOf("getPackagesForUid")

        private val INFO_METHODS = setOf(
            "getActivityInfo",
            "getApplicationInfo",
            "getPackageInfo",
            "getProviderInfo",
            "getReceiverInfo",
            "getServiceInfo",
            "resolveActivity",
            "resolveContentProvider",
            "resolveService",
        )

        private val NAME_NOT_FOUND_METHODS = setOf(
            "getActivityInfo",
            "getApplicationInfo",
            "getPackageInfo",
            "getProviderInfo",
            "getReceiverInfo",
            "getServiceInfo",
        )
    }
}
