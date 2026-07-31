package com.houvven.guise.module.preset

import com.houvven.guise.module.PresetAdapter

enum class DensityPreset(override val label: String, override val value: String) : PresetAdapter {
    LDPI("120 dpi (ldpi)", "120"),
    MDPI("160 dpi (mdpi)", "160"),
    TVDPI("213 dpi (tvdpi)", "213"),
    HDPI("240 dpi (hdpi)", "240"),
    DPI_280("280 dpi", "280"),
    XHDPI("320 dpi (xhdpi)", "320"),
    DPI_360("360 dpi", "360"),
    DPI_400("400 dpi", "400"),
    DPI_420("420 dpi", "420"),
    DPI_440("440 dpi", "440"),
    XXHDPI("480 dpi (xxhdpi)", "480"),
    DPI_560("560 dpi", "560"),
    XXXHDPI("640 dpi (xxxhdpi)", "640"),
}
