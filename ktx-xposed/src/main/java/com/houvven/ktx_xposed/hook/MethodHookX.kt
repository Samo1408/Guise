@file:Suppress("unused", "UNCHECKED_CAST")

package com.houvven.ktx_xposed.hook

import com.houvven.ktx_xposed.utils.runXposedCatching
import io.github.libxposed.api.XposedInterface.HookHandle
import java.lang.reflect.Executable

object HookType {
    const val BEFORE = 0
    const val AFTER = 1
}

class MethodHookParam internal constructor(
    val thisObject: Any?,
    val args: Array<Any?>,
    initialResult: Any? = null,
) {
    internal var resultWasSet = false
    private var storedResult: Any? = initialResult

    var result: Any?
        get() = storedResult
        set(value) {
            storedResult = value
            resultWasSet = true
        }
}

private fun beforeHook(
    executable: Executable,
    callback: (MethodHookParam) -> Unit,
): HookHandle = ModernXposedRuntime.module.hook(executable).intercept { chain ->
    val param = MethodHookParam(chain.thisObject, chain.args.toTypedArray())
    callback(param)
    if (param.resultWasSet) param.result else chain.proceed(param.args as Array<Any>)
}

private fun afterHook(
    executable: Executable,
    callback: (MethodHookParam) -> Unit,
): HookHandle = ModernXposedRuntime.module.hook(executable).intercept { chain ->
    val originalResult = chain.proceed()
    val param = MethodHookParam(chain.thisObject, chain.args.toTypedArray(), originalResult)
    callback(param)
    if (param.resultWasSet) param.result else originalResult
}

fun Class<*>.beforeHookedMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
): HookHandle? = runXposedCatching { beforeHook(findMethodExact(methodName, *parameterTypes), callback) }

fun Class<*>.afterHookedMethod(
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
): HookHandle? = runXposedCatching { afterHook(findMethodExact(methodName, *parameterTypes), callback) }

fun beforeHookedMethod(
    className: String,
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).beforeHookedMethod(methodName, *parameterTypes, callback = callback)

fun afterHookedMethod(
    className: String,
    methodName: String,
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).afterHookedMethod(methodName, *parameterTypes, callback = callback)

fun Class<*>.beforeHookAllMethods(
    methodName: String,
    callback: (MethodHookParam) -> Unit,
): Set<HookHandle> = allMethods()
    .filter { it.name == methodName }
    .mapNotNull { runXposedCatching { beforeHook(it, callback) } }
    .toSet()

fun Class<*>.afterHookAllMethods(
    methodName: String,
    callback: (MethodHookParam) -> Unit,
): Set<HookHandle> = allMethods()
    .filter { it.name == methodName }
    .mapNotNull { runXposedCatching { afterHook(it, callback) } }
    .toSet()

fun beforeHookAllMethods(
    className: String,
    methodName: String,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).beforeHookAllMethods(methodName, callback)

fun afterHookAllMethods(
    className: String,
    methodName: String,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).afterHookAllMethods(methodName, callback)

fun Class<*>.beforeHookConstructor(
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
): HookHandle? = runXposedCatching {
    beforeHook(getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }, callback)
}

fun Class<*>.afterHookConstructor(
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
): HookHandle? = runXposedCatching {
    afterHook(getDeclaredConstructor(*parameterTypes).apply { isAccessible = true }, callback)
}

fun beforeHookConstructor(
    className: String,
    vararg parameterTypes: Class<*>,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).beforeHookConstructor(*parameterTypes, callback = callback)

fun Class<*>.beforeHookSomeSameNameMethod(
    vararg methodName: String,
    callback: (MethodHookParam) -> Unit,
) = methodName.map { beforeHookAllMethods(it, callback) }.toSet()

fun Class<*>.afterHookSomeSameNameMethod(
    vararg methodName: String,
    callback: (MethodHookParam) -> Unit,
) = methodName.map { afterHookAllMethods(it, callback) }.toSet()

fun beforeHookSomeSameNameMethod(
    className: String,
    vararg methodName: String,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).beforeHookSomeSameNameMethod(*methodName, callback = callback)

fun afterHookSomeSameNameMethod(
    className: String,
    vararg methodName: String,
    callback: (MethodHookParam) -> Unit,
) = findClass(className).afterHookSomeSameNameMethod(*methodName, callback = callback)

fun setMethodResult(
    className: String,
    methodName: String,
    value: Any?,
    type: Int = HookType.BEFORE,
    vararg parameterTypes: Class<*>,
) = findClass(className).setMethodResult(methodName, value, type, *parameterTypes)

fun Class<*>.setMethodResult(
    methodName: String,
    value: Any?,
    type: Int = HookType.BEFORE,
    vararg parameterTypes: Class<*>,
) = if (type == HookType.BEFORE) {
    beforeHookedMethod(methodName, *parameterTypes) { it.result = value }
} else {
    afterHookedMethod(methodName, *parameterTypes) { it.result = value }
}

fun Class<*>.setAllMethodResult(
    methodName: String,
    value: Any?,
    type: Int = HookType.BEFORE,
) = if (type == HookType.BEFORE) {
    beforeHookAllMethods(methodName) { it.result = value }
} else {
    afterHookAllMethods(methodName) { it.result = value }
}

fun setAllMethodResult(
    className: String,
    methodName: String,
    value: Any?,
    type: Int = HookType.BEFORE,
) = findClass(className).setAllMethodResult(methodName, value, type)

fun Class<*>.setSomeSameNameMethodResult(
    vararg methodName: String,
    value: Any?,
    type: Int = HookType.BEFORE,
) = if (type == HookType.BEFORE) {
    beforeHookSomeSameNameMethod(*methodName) { it.result = value }
} else {
    afterHookSomeSameNameMethod(*methodName) { it.result = value }
}

fun setSomeSameNameMethodResultForAnyClass(
    classAndMethodName: List<Pair<Class<*>, String>>,
    value: Any?,
    type: Int = HookType.BEFORE,
) = classAndMethodName.map { (clazz, method) ->
    if (type == HookType.BEFORE) clazz.beforeHookAllMethods(method) { it.result = value }
    else clazz.afterHookAllMethods(method) { it.result = value }
}
