package com.houvven.ktx_xposed.hook

import com.houvven.ktx_xposed.utils.runXposedCatching
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private fun Class<*>.matchingMethod(methodName: String, args: Array<out Any?>): Method =
    allMethods().firstOrNull { method ->
        method.name == methodName && method.parameterTypes.size == args.size &&
            method.parameterTypes.zip(args).all { (type, value) -> value == null || type.boxed().isInstance(value) }
    }?.apply { isAccessible = true } ?: throw NoSuchMethodException("$name#$methodName")

private fun Class<*>.boxed(): Class<*> = when (this) {
    Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
    Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
    Char::class.javaPrimitiveType -> Char::class.javaObjectType
    Short::class.javaPrimitiveType -> Short::class.javaObjectType
    Int::class.javaPrimitiveType -> Int::class.javaObjectType
    Long::class.javaPrimitiveType -> Long::class.javaObjectType
    Float::class.javaPrimitiveType -> Float::class.javaObjectType
    Double::class.javaPrimitiveType -> Double::class.javaObjectType
    else -> this
}

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? =
    runXposedCatching { matchingMethod(methodName, args).invoke(null, *args) }

fun Class<*>.callStaticMethod(
    methodName: String,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?,
): Any? = runXposedCatching { findMethodExact(methodName, *parameterTypes).invoke(null, *args) }

fun Any.callMethod(methodName: String, vararg args: Any?): Any? =
    runXposedCatching { javaClass.matchingMethod(methodName, args).invoke(this, *args) }

fun Any.callMethod(
    methodName: String,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?,
): Any? = runXposedCatching { javaClass.findMethodExact(methodName, *parameterTypes).invoke(this, *args) }

fun Any.callMethodIfExists(methodName: String, vararg args: Any): Any? = callMethod(methodName, *args)
fun Any.callMethodIfExists(methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any): Any? =
    callMethod(methodName, parameterTypes, *args)
fun Class<*>.callStaticMethodIfExists(methodName: String, vararg args: Any): Any? = callStaticMethod(methodName, *args)
fun Class<*>.callStaticMethodIfExists(methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any): Any? =
    callStaticMethod(methodName, parameterTypes, *args)
