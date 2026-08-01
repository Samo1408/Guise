package com.houvven.guise.module.apps

data class AppInfo(
    var isEnable: Boolean,
    val label: String,
    val packageName: String,
    val installTime: Long,
    val updateTime: Long,
    val isSystemApp: Boolean,
)
