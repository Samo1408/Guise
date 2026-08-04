package com.houvven.ktx_xposed.utils

import com.houvven.ktx_xposed.logger.XposedLogger

inline fun <R> runXposedCatching(category: String? = null, block: () -> R): R? {
    return try {
        block()
    } catch (e: Throwable) {
        XposedLogger.e(e, category ?: XposedLogger.currentCategory)
        null
    }
}
