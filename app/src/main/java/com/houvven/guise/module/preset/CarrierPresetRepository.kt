package com.houvven.guise.module.preset

import android.content.Context
import com.houvven.guise.module.PresetAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CarrierPreset(
    val name: String,
    val plmn: String,
    val countryCode: String,
    val countryName: String,
) : PresetAdapter {
    override val label: String get() = "$countryName · $name · $plmn"
    override val value: String get() = plmn
}

object CarrierPresetRepository {
    @Volatile
    private var cached: List<CarrierPreset>? = null

    fun get(context: Context): List<CarrierPreset> = cached ?: synchronized(this) {
        cached ?: context.assets.open("carriers.json").bufferedReader().use { reader ->
            Json.decodeFromString<List<CarrierPreset>>(reader.readText()).also { cached = it }
        }
    }
}
