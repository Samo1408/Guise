package com.houvven.guise.update

import android.content.Context
import android.util.Base64
import com.houvven.guise.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val notes: String,
    val releaseUrl: String,
    val apkUrl: String,
) {
    val actionUrl: String get() = apkUrl.ifBlank { releaseUrl }
}

class AppUpdater {
    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        coroutineScope {
            val attempts = UPDATE_SOURCES.map { source ->
                async { runCatching { fetch(source) } }
            }

            // GitHub is authoritative. Mirrors are started at the same time so a GitHub failure
            // can immediately fall back without making the user wait through serial timeouts.
            val authoritative = attempts.first().await()
            authoritative.getOrNull()?.let { info ->
                attempts.drop(1).forEach { it.cancel() }
                return@coroutineScope info
            }

            val fallbacks = attempts.drop(1).awaitAll()
            fallbacks.mapNotNull(Result<UpdateInfo>::getOrNull)
                .maxByOrNull(UpdateInfo::versionCode)
                ?.let { return@coroutineScope it }

            throw IllegalStateException(
                fallbacks.mapNotNull(Result<UpdateInfo>::exceptionOrNull)
                    .lastOrNull()?.message
                    ?: authoritative.exceptionOrNull()?.message,
            )
        }
    }

    fun isNewer(info: UpdateInfo): Boolean = info.versionCode > BuildConfig.VERSION_CODE

    private fun fetch(source: String): UpdateInfo {
        val separator = if ('?' in source) '&' else '?'
        val connection = URL("$source${separator}t=${System.currentTimeMillis()}")
            .openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.setRequestProperty("User-Agent", "Guise/${BuildConfig.VERSION_CODE}")
        return try {
            check(connection.responseCode in 200..299) {
                "HTTP ${connection.responseCode}"
            }
            var json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optString("encoding").equals("base64", ignoreCase = true)) {
                val decoded = Base64.decode(json.getString("content"), Base64.DEFAULT)
                json = JSONObject(decoded.toString(Charsets.UTF_8))
            }
            parse(json)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: JSONObject): UpdateInfo {
        val tag = json.optString("tag")
        val versionName = json.optString("versionName")
            .ifBlank { json.optString("version") }
            .ifBlank { tag.removePrefix("v") }
        val versionCode = json.optInt("versionCode", -1)
        check(versionCode > 0) { "Invalid versionCode" }
        check(versionName.isNotBlank()) { "Missing versionName" }
        val releaseUrl = json.optString("releaseUrl")
            .ifBlank { RELEASES_URL }
        check(releaseUrl.startsWith("https://")) { "Invalid releaseUrl" }
        val apkUrl = json.optString("apkUrl")
        check(apkUrl.isBlank() || apkUrl.startsWith("https://")) { "Invalid apkUrl" }
        return UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            title = json.optString("title").ifBlank {
                json.optString("name").ifBlank { tag }
            },
            notes = json.optString("notes").lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .joinToString("\n") { if (it.startsWith("- ")) "• ${it.drop(2)}" else it },
            releaseUrl = releaseUrl,
            apkUrl = apkUrl,
        )
    }

    companion object {
        const val RELEASES_URL = "https://github.com/daxiaamu/Guise_Reborn/releases"
        private const val MANIFEST_PATH = "daxiaamu/Guise_Reborn@main/latest-release.json"
        private const val CONNECT_TIMEOUT_MS = 6_000
        private const val READ_TIMEOUT_MS = 8_000
        private val UPDATE_SOURCES = listOf(
            "https://api.github.com/repos/daxiaamu/Guise_Reborn/contents/latest-release.json?ref=main",
            "https://cdn.jsdelivr.net/gh/$MANIFEST_PATH",
            "https://fastly.jsdelivr.net/gh/$MANIFEST_PATH",
            "https://gcore.jsdelivr.net/gh/$MANIFEST_PATH",
            "https://raw.githubusercontent.com/daxiaamu/Guise_Reborn/main/latest-release.json",
        )
    }
}

object UpdatePromptPreferences {
    private const val PREFERENCES = "app_update"
    private const val KEY_IGNORED_VERSION = "ignored_version_code"

    fun ignore(context: Context, versionCode: Int) {
        preferences(context).edit().putInt(KEY_IGNORED_VERSION, versionCode).apply()
    }

    fun isIgnored(context: Context, versionCode: Int): Boolean =
        preferences(context).getInt(KEY_IGNORED_VERSION, 0) >= versionCode

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
