package com.houvven.ktx_xposed.utils

import com.houvven.ktx_xposed.hook.MethodHookParam

fun MethodHookParam.setNullResult() {
    result = null
}

fun MethodHookParam.hasTypeArg(type: Class<*>): Boolean = args.any { it?.javaClass == type }

fun MethodHookParam.getTypeArgIndexOfFirst(type: Class<*>): Int =
    args.indexOfFirst { it?.javaClass == type }
