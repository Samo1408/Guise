@file:Suppress("unused")

package com.houvven.ktx_xposed.hook

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

fun Class<*>.findField(fieldName: String): Field {
    var current: Class<*>? = this
    while (current != null) {
        runCatching { current.getDeclaredField(fieldName) }.getOrNull()?.let {
            it.isAccessible = true
            return it
        }
        current = current.superclass
    }
    throw NoSuchFieldException("$name#$fieldName")
}

fun Class<*>.findFiledIfExists(fieldName: String): Field? = runCatching { findField(fieldName) }.getOrNull()

fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field? =
    generateSequence(this) { it.superclass }
        .flatMap { it.declaredFields.asSequence() }
        .firstOrNull { it.type == type }
        ?.apply { isAccessible = true }

fun Class<*>.findMethodExact(name: String, vararg parameterTypes: Class<*>): Method {
    var current: Class<*>? = this
    while (current != null) {
        runCatching { current.getDeclaredMethod(name, *parameterTypes) }.getOrNull()?.let {
            it.isAccessible = true
            return it
        }
        current = current.superclass
    }
    throw NoSuchMethodException("${this.name}#$name")
}

fun Class<*>.findMethodExactIfExists(name: String, vararg parameterTypes: Class<*>): Method? =
    runCatching { findMethodExact(name, *parameterTypes) }.getOrNull()

fun Class<*>.findMethodBestMatch(name: String, vararg parameterTypes: Class<*>): Method =
    allMethods().firstOrNull { method ->
        method.name == name && method.parameterTypes.size == parameterTypes.size &&
            method.parameterTypes.zip(parameterTypes).all { (expected, actual) -> expected.isAssignableFrom(actual) }
    }?.apply { isAccessible = true } ?: throw NoSuchMethodException("${this.name}#$name")

fun Class<*>.findMethodBestMatch(name: String, vararg parameterTypes: Any): Method =
    findMethodBestMatch(name, *parameterTypes.map { it as Class<*> }.toTypedArray())

fun Class<*>.findMethodBestMatch(
    name: String,
    parameterTypes: Array<Class<*>>,
    args: Array<Any>,
): Method? = runCatching { findMethodBestMatch(name, *parameterTypes) }.getOrNull()

fun Class<*>.findMethodsByExactParameters(
    returnType: Class<*>,
    vararg parameterTypes: Class<*>,
): Array<Method> = allMethods().filter {
    it.returnType == returnType && it.parameterTypes.contentEquals(parameterTypes)
}.toList().toTypedArray()

internal fun Class<*>.allMethods(): Sequence<Method> =
    generateSequence(this) { it.superclass }
        .flatMap { it.declaredMethods.asSequence() }
        .distinctBy { method -> method.name to method.parameterTypes.toList() }
