package com.houvven.guise.xposed.hook

import android.icu.util.TimeZone as IcuTimeZone
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import java.time.ZoneId
import java.util.TimeZone as JavaTimeZone

class TimeZoneHook : LoadPackageHandler {

    override fun onHook() {
        val id = config.timeZone
        if (id.isBlank()) return

        val zoneId = runCatching { ZoneId.of(id) }.getOrNull() ?: return
        val javaTimeZone = JavaTimeZone.getTimeZone(zoneId)
        val icuTimeZone = IcuTimeZone.getTimeZone(id)

        // Set process defaults for APIs that cache the zone during initialization.
        JavaTimeZone.setDefault(javaTimeZone)

        JavaTimeZone::class.java.beforeHookedMethod("getDefault") { param ->
            param.result = javaTimeZone.clone()
        }
        JavaTimeZone::class.java.beforeHookedMethod("getDefaultRef") { param ->
            param.result = javaTimeZone.clone()
        }
        IcuTimeZone::class.java.beforeHookedMethod("getDefault") { param ->
            param.result = icuTimeZone.cloneAsThawed()
        }
        ZoneId::class.java.beforeHookedMethod("systemDefault") { param ->
            param.result = zoneId
        }
    }
}
