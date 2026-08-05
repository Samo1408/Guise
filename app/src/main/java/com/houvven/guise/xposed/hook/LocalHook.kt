package com.houvven.guise.xposed.hook

import android.os.Handler
import android.os.Looper
import com.houvven.guise.xposed.LoadPackageHandler
import java.util.Locale

class LocalHook : LoadPackageHandler {

    override fun onHook() {
        var language = config.language
        var country: String

        if (language.isBlank()) return

        language.split("_").let {
            language = it[0]
            country = if (it.size < 2) "" else it[1]
        }

        val locale = runCatching {
            Locale.Builder().setLanguage(language).apply {
                if (country.isNotBlank()) setRegion(country)
            }.build()
        }.getOrNull() ?: return

        setProcessDefault(locale)
        // ActivityThread initializes the app locale after PackageReady. Reapply once after the
        // current bind-application message instead of keeping getter hooks in the process.
        Handler(Looper.getMainLooper()).post { setProcessDefault(locale) }
    }

    private fun setProcessDefault(locale: Locale) = Locale.setDefault(locale)
}
