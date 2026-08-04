package com.houvven.ktx_xposed.hook

import io.github.libxposed.api.XposedModule

data class LoadPackageContext(
    val packageName: String,
    val processName: String,
    val classLoader: ClassLoader,
)

object ModernXposedRuntime {
    lateinit var module: XposedModule
    lateinit var packageContext: LoadPackageContext

    val moduleOrNull: XposedModule?
        get() = if (::module.isInitialized) module else null

    val packageContextOrNull: LoadPackageContext?
        get() = if (::packageContext.isInitialized) packageContext else null

    fun initialize(module: XposedModule, context: LoadPackageContext) {
        this.module = module
        packageContext = context
    }
}

val lppram: LoadPackageContext
    get() = ModernXposedRuntime.packageContext

val classLoader: ClassLoader
    get() = lppram.classLoader

fun findClass(className: String): Class<*> = Class.forName(className, false, classLoader)

fun findClassIfExists(className: String): Class<*>? = runCatching { findClass(className) }.getOrNull()
