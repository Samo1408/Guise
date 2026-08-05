package com.houvven.guise.ui.utils

import android.content.Context
import com.houvven.guise.db.DeviceDBHelper
import com.houvven.guise.module.preset.CarrierPreset
import com.houvven.guise.module.preset.CarrierPresetRepository
import com.houvven.guise.module.preset.PresetRepository
import com.houvven.guise.module.preset.TimeZonePresetRepository
import com.houvven.guise.util.android.Randoms
import com.houvven.guise.xposed.config.ModuleConfigState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun oneClickRandom(state: ModuleConfigState, context: Context) {
    val values = withContext(Dispatchers.IO) {
        val (brand, device) = DeviceDBHelper(context).use { deviceDB ->
            val selectedBrand = deviceDB.getAllBrand().keys.random()
            selectedBrand to deviceDB.getDevicesByBrand(selectedBrand).random()
        }
        val android = PresetRepository.get(context).androidVersions
            .filter { it.value.substringAfter('|').toIntOrNull()?.let { api -> api >= 29 } == true }
            .random()
        val carrier = CarrierPresetRepository.get(context).random()
        RandomSelection(
            brand = brand,
            model = device.model.orEmpty(),
            device = device.codeAlias?.takeIf(String::isNotBlank) ?: device.code.orEmpty(),
            android = android.value,
            carrier = carrier,
        )
    }

    state.run {
        val productCode = values.device.ifBlank { values.model.fingerprintSafePart() }
        val version = values.android.substringBefore('|')
        val api = values.android.substringAfter('|')
        val generatedBuildId = Randoms.randomBuildId(version)

        brand.value = values.brand
        manufacturer.value = values.brand
        model.value = values.model
        device.value = values.device
        product.value = productCode
        androidVersion.value = version
        sdkInt.value = api
        buildId.value = generatedBuildId
        fingerPrint.value = Randoms.randomFingerprint(
            brand = values.brand,
            product = productCode,
            device = values.device,
            androidVersion = version,
            buildId = generatedBuildId,
        )

        networkType.value = PresetRepository.get(context).networks.random().value

        wifiSSID.value = Randoms.randomString(10)
        wifiBSSID.value = Randoms.randomMacAddress()
        wifiMacAddress.value = Randoms.randomMacAddress()

        Randoms.randomCoordinates().let { (lat, lon) ->
            latitude.value = lat.toString()
            longitude.value = lon.toString()
        }

        simOperatorName.value = values.carrier.name
        simOperator.value = values.carrier.plmn
        simCountry.value = values.carrier.countryCode

        androidId.value = Randoms.randomAndroidId()
        imei.value = Randoms.randomIMEI()
        phoneNum.value = Randoms.randomPhoneNum()

        batteryLevel.value = Randoms.randomBatteryLevel().toString()
        timeZone.value = TimeZonePresetRepository.randomId()
    }
}

private data class RandomSelection(
    val brand: String,
    val model: String,
    val device: String,
    val android: String,
    val carrier: CarrierPreset,
)

private fun String.fingerprintSafePart(): String =
    trim().replace(Regex("[\\s/:]+"), "_").ifBlank { "device" }
