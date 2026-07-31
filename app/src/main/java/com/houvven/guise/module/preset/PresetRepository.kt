package com.houvven.guise.module.preset

import android.content.Context
import com.houvven.guise.R
import com.houvven.guise.module.PresetAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ResourcePreset(
    override val label: String,
    override val value: String,
) : PresetAdapter

@Serializable
data class PresetCatalog(
    val androidVersions: List<ResourcePreset>,
    val sdkLevels: List<ResourcePreset>,
    val densityDpi: List<ResourcePreset>,
    val networks: List<ResourcePreset>,
    val languages: List<ResourcePreset>,
)

object PresetRepository {
    @Volatile
    private var cached: PresetCatalog? = null

    fun get(context: Context): PresetCatalog = cached ?: synchronized(this) {
        cached ?: context.resources.openRawResource(R.raw.presets).bufferedReader().use { reader ->
            Json.decodeFromString<PresetCatalog>(reader.readText()).also { cached = it }
        }
    }
}
