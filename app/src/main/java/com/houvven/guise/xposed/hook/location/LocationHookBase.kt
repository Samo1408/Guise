package com.houvven.guise.xposed.hook.location

import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.INCLUDE_LOCATION_DATA_NONE
import android.telephony.gsm.GsmCellLocation
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.hook.setMethodResult

@Suppress("DEPRECATION")
open class LocationHookBase {

    protected fun makeCellLocationFail() {
        TelephonyManager::class.java.run {
            setMethodResult("getCellLocation", null)
            setMethodResult("getAllCellInfo", null)
            setMethodResult("getNeighboringCellInfo", null)
            setMethodResult("getLastKnownCellIdentity", null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setMethodResult("getLocationData", INCLUDE_LOCATION_DATA_NONE)
            }
        }

        GsmCellLocation::class.java.run {
            setMethodResult("getPsc", -1)
            setMethodResult("getLac", -1)
        }
    }

    protected fun makeWifiLocationFail() {
        WifiManager::class.java.run {
            setMethodResult("getScanResults", emptyList<ScanResult>())
            setMethodResult("isScanAlwaysAvailable", false)
        }

        val hideNetworkProvider: (com.houvven.ktx_xposed.hook.MethodHookParam) -> Unit = { param ->
            when (param.args.firstOrNull() as? String) {
                LocationManager.NETWORK_PROVIDER,
                LocationManager.FUSED_PROVIDER,
                -> param.result = false
            }
        }
        LocationManager::class.java.beforeHookedMethod(
            "isProviderEnabled",
            String::class.java,
            callback = hideNetworkProvider,
        )
        LocationManager::class.java.beforeHookedMethod(
            "hasProvider",
            String::class.java,
            callback = hideNetworkProvider,
        )
    }

}
