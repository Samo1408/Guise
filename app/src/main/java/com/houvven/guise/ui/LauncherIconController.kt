package com.houvven.guise.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal object LauncherIconController {
    private const val LAUNCHER_ALIAS = "com.houvven.guise.ui.LauncherActivityAlias"

    fun isHidden(context: Context): Boolean =
        when (context.packageManager.getComponentEnabledSetting(component(context))) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> true

            else -> false
        }

    fun setHidden(context: Context, hidden: Boolean): Boolean {
        val targetState = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            component(context),
            targetState,
            PackageManager.DONT_KILL_APP,
        )
        return isHidden(context)
    }

    private fun component(context: Context) = ComponentName(context, LAUNCHER_ALIAS)
}
