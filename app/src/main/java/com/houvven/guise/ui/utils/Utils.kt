package com.houvven.guise.ui.utils

import android.content.Context
import com.houvven.guise.db.DeviceDBHelper
import com.houvven.guise.module.ktx.runThread
import com.houvven.guise.module.preset.CarrierPresetRepository
import com.houvven.guise.module.preset.PresetRepository
import com.houvven.guise.module.preset.TimeZonePresetRepository
import com.houvven.guise.util.android.Randoms
import com.houvven.guise.xposed.config.ModuleConfigState

fun oneClickRandom(state: ModuleConfigState, context: Context) {
    runThread {
        val deviceDB = DeviceDBHelper(context)
        val rBrand = deviceDB.getAllBrand().keys.random()
        val rDevice = deviceDB.getDevicesByBrand(rBrand).random()
        deviceDB.close()

        state.run {
            brand.value = rBrand
            model.value = rDevice.model ?: ""
            device.value = rDevice.codeAlias ?: ""

            fingerPrint.value = Randoms.randomFingerPrint()

            networkType.value = PresetRepository.get(context).networks.random().value

            wifiSSID.value = Randoms.randomString(10)
            wifiBSSID.value = Randoms.randomMacAddress()
            wifiMacAddress.value = Randoms.randomMacAddress()

            Randoms.randomLatLac().let {
                latitude.value = it.x.toString()
                longitude.value = it.y.toString()
            }

            CarrierPresetRepository.get(context).random().let {
                simOperatorName.value = it.name
                simOperator.value = it.plmn
                simCountry.value = it.countryCode
            }

            androidId.value = Randoms.randomIMEI()
            imei.value = Randoms.randomIMEI()
            phoneNum.value = Randoms.randomPhoneNum()

            batteryLevel.value = Randoms.randomBatteryLevel().toString()
            timeZone.value = TimeZonePresetRepository.randomId()
        }
    }

}
