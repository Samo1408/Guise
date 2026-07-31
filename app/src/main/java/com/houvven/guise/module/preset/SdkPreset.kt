package com.houvven.guise.module.preset

import com.houvven.guise.module.PresetAdapter

enum class SdkPreset(
    private val androidVersion: String,
    override val value: String,
) : PresetAdapter {
    API_1("Android 1.0", "1"),
    API_2("Android 1.1", "2"),
    API_3("Android 1.5", "3"),
    API_4("Android 1.6", "4"),
    API_5("Android 2.0", "5"),
    API_6("Android 2.0.1", "6"),
    API_7("Android 2.1", "7"),
    API_8("Android 2.2", "8"),
    API_9("Android 2.3–2.3.2", "9"),
    API_10("Android 2.3.3–2.3.7", "10"),
    API_11("Android 3.0", "11"),
    API_12("Android 3.1", "12"),
    API_13("Android 3.2", "13"),
    API_14("Android 4.0–4.0.2", "14"),
    API_15("Android 4.0.3–4.0.4", "15"),
    API_16("Android 4.1", "16"),
    API_17("Android 4.2", "17"),
    API_18("Android 4.3", "18"),
    API_19("Android 4.4", "19"),
    API_20("Android 4.4W", "20"),
    API_21("Android 5.0", "21"),
    API_22("Android 5.1", "22"),
    API_23("Android 6", "23"),
    API_24("Android 7", "24"),
    API_25("Android 7.1", "25"),
    API_26("Android 8", "26"),
    API_27("Android 8.1", "27"),
    API_28("Android 9", "28"),
    API_29("Android 10", "29"),
    API_30("Android 11", "30"),
    API_31("Android 12", "31"),
    API_32("Android 12L", "32"),
    API_33("Android 13", "33"),
    API_34("Android 14", "34"),
    API_35("Android 15", "35"),
    API_36("Android 16", "36"),
    API_37("Android 17", "37"),
    ;

    override val label: String = "API $value · $androidVersion"
}
