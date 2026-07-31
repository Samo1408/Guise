package com.houvven.ktx_xposed.hook

import com.houvven.ktx_xposed.utils.runXposedCatching

inline fun <reified T> setStaticField(className: String, fieldName: String, value: T) {
    findClass(className).setStaticField(fieldName, value)
}

inline fun <reified T> Class<*>.setStaticField(fieldName: String, value: T) {
    runXposedCatching { findField(fieldName).set(null, value) }
}

inline fun <reified T> setInstanceField(instance: Any, fieldName: String, value: T) {
    runXposedCatching { instance.javaClass.findField(fieldName).set(instance, value) }
}
