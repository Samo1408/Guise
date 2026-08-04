package com.houvven.guise.xposed.hook

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.guise.xposed.config.HooksValue
import com.houvven.ktx_xposed.hook.afterHookedMethod
import com.houvven.ktx_xposed.hook.beforeHookAllMethods
import com.houvven.ktx_xposed.hook.findClassIfExists

class ScreenshotsHook : LoadPackageHandler {

    override fun onHook() {
        when (config.screenshotsFlag) {
            HooksValue.SCREENSHOTS_DISABLE -> disableScreenshots()
            HooksValue.SCREENSHOTS_ENABLE -> enableScreenshots()
        }
    }

    private fun disableScreenshots() {
        Activity::class.java.afterHookedMethod(
            methodName = "onCreate",
            Bundle::class.java,
        ) { param ->
            (param.thisObject as? Activity)?.window?.addFlags(FLAG_SECURE)
        }
    }

    private fun enableScreenshots() {
        interceptWindowFlags()
        interceptWindowLayoutParams()
        clearFlagsFromActivityLifecycle()
    }

    /**
     * Prevent future Window calls from adding FLAG_SECURE. The old implementation only compared
     * the complete flags value with FLAG_SECURE, so combined flags slipped through unchanged.
     */
    private fun interceptWindowFlags() {
        Window::class.java.beforeHookAllMethods("setFlags") { param ->
            val flags = param.args.getOrNull(0) as? Int ?: return@beforeHookAllMethods
            val mask = param.args.getOrNull(1) as? Int ?: return@beforeHookAllMethods
            if (mask containsFlag FLAG_SECURE) {
                param.args[0] = flags withoutFlag FLAG_SECURE
            }
        }
        Window::class.java.beforeHookAllMethods("addFlags") { param ->
            val flags = param.args.getOrNull(0) as? Int ?: return@beforeHookAllMethods
            param.args[0] = flags withoutFlag FLAG_SECURE
        }
    }

    /** Covers Window#setAttributes and direct WindowManager add/update calls. */
    private fun interceptWindowLayoutParams() {
        Window::class.java.beforeHookAllMethods("setAttributes") { param ->
            param.args.firstOrNull().clearSecureFlag()
        }

        findClassIfExists("android.view.WindowManagerImpl")?.let { windowManagerImpl ->
            listOf("addView", "updateViewLayout").forEach { methodName ->
                windowManagerImpl.beforeHookAllMethods(methodName) { param ->
                    param.args.forEach { argument -> argument.clearSecureFlag() }
                }
            }
        }
    }

    /** Removes a secure flag that was already present before hooks or lifecycle callbacks ran. */
    private fun clearFlagsFromActivityLifecycle() {
        val clearWindow = { activity: Activity ->
            runCatching {
                if (activity.window.attributes.flags containsFlag FLAG_SECURE) {
                    activity.window.clearFlags(FLAG_SECURE)
                }
            }
            Unit
        }
        Activity::class.java.afterHookedMethod("onCreate", Bundle::class.java) { param ->
            (param.thisObject as? Activity)?.let(clearWindow)
        }
        Activity::class.java.afterHookedMethod("onResume") { param ->
            (param.thisObject as? Activity)?.let(clearWindow)
        }
    }

    private fun Any?.clearSecureFlag() {
        (this as? WindowManager.LayoutParams)?.let { attributes ->
            attributes.flags = attributes.flags withoutFlag FLAG_SECURE
        }
    }

    private infix fun Int.containsFlag(flag: Int): Boolean = this and flag != 0

    private infix fun Int.withoutFlag(flag: Int): Int = this and flag.inv()

    private companion object {
        const val FLAG_SECURE = WindowManager.LayoutParams.FLAG_SECURE
    }
}
