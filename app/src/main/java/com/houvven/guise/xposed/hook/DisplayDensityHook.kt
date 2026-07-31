@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.hook

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.Display
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookAllMethods
import kotlin.math.roundToInt

class DisplayDensityHook : LoadPackageHandler {

    @SuppressLint("WrongConstant")
    override fun onHook() {
        val densityDpi = config.densityDpi
        if (densityDpi !in MIN_DENSITY_DPI..MAX_DENSITY_DPI) return

        fun applyDensity(metrics: DisplayMetrics) {
            val fontScale = if (metrics.density > 0f) {
                metrics.scaledDensity / metrics.density
            } else {
                1f
            }
            metrics.densityDpi = densityDpi
            metrics.density = densityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()
            metrics.scaledDensity = metrics.density * fontScale
        }

        fun applyDensity(configuration: Configuration) {
            val originalDensityDpi = configuration.densityDpi
            if (originalDensityDpi > 0 && originalDensityDpi != densityDpi) {
                val scale = originalDensityDpi.toFloat() / densityDpi
                configuration.screenWidthDp = configuration.screenWidthDp.scaledBy(scale)
                configuration.screenHeightDp = configuration.screenHeightDp.scaledBy(scale)
                configuration.smallestScreenWidthDp =
                    configuration.smallestScreenWidthDp.scaledBy(scale)
            }
            configuration.densityDpi = densityDpi
        }

        Resources.getSystem().run {
            applyDensity(displayMetrics)
            applyDensity(configuration)
        }

        Resources::class.java.afterHookAllMethods("getDisplayMetrics") { param ->
            (param.result as? DisplayMetrics)?.let(::applyDensity)
        }
        Resources::class.java.afterHookAllMethods("getConfiguration") { param ->
            (param.result as? Configuration)?.let(::applyDensity)
        }
        Display::class.java.afterHookAllMethods("getMetrics") { param ->
            (param.args.firstOrNull() as? DisplayMetrics)?.let(::applyDensity)
        }
        Display::class.java.afterHookAllMethods("getRealMetrics") { param ->
            (param.args.firstOrNull() as? DisplayMetrics)?.let(::applyDensity)
        }
    }

    private companion object {
        const val MIN_DENSITY_DPI = 72
        const val MAX_DENSITY_DPI = 1000

        fun Int.scaledBy(scale: Float): Int =
            if (this == Configuration.SCREEN_WIDTH_DP_UNDEFINED) this
            else (this * scale).roundToInt()
    }
}
