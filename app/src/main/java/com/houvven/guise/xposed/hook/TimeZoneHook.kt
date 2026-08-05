package com.houvven.guise.xposed.hook

import android.os.Handler
import android.os.Looper
import com.houvven.guise.xposed.LoadPackageHandler
import java.time.ZoneId
import java.util.TimeZone as JavaTimeZone

class TimeZoneHook : LoadPackageHandler {

    override fun onHook() {
        val id = config.timeZone
        if (id.isBlank()) return

        val zoneId = runCatching { ZoneId.of(id) }.getOrNull() ?: return
        val javaTimeZone = JavaTimeZone.getTimeZone(zoneId)

        // Android's java.time and ICU-backed formatting derive their process default from
        // java.util.TimeZone. Explicit, non-default TimeZone instances remain intact.
        setProcessDefault(javaTimeZone)
        // ActivityThread may initialize the process default after PackageReady. Reapply once
        // after the current bind-application message without retaining TimeZone method hooks.
        Handler(Looper.getMainLooper()).post { setProcessDefault(javaTimeZone) }
    }

    private fun setProcessDefault(timeZone: JavaTimeZone) = JavaTimeZone.setDefault(timeZone)
}
