package com.houvven.guise.module.preset

import java.time.ZoneId

object TimeZonePresetRepository {
    val presets: List<ResourcePreset> by lazy {
        ZoneId.getAvailableZoneIds()
            .sorted()
            .map { id -> ResourcePreset(label = id, value = id) }
    }

    fun randomId(): String = presets.random().value
}
