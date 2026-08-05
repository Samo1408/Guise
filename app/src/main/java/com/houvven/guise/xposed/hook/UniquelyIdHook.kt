@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.hook

import android.content.ContentResolver
import android.provider.Settings
import android.provider.Settings.Secure
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.hook.setAllMethodResult
import com.houvven.ktx_xposed.hook.setMethodResult

class UniquelyIdHook : LoadPackageHandler {

    override fun onHook() {
        if (config.androidId.isNotBlank()) this.hookAndroidId()
        if (config.imei.isNotBlank()) this.hookImei()
        if (config.phoneNum.isNotBlank()) this.hookPhoneNum()
    }

    private fun hookAndroidId() {
        Secure::class.java.beforeHookedMethod(
            methodName = "getStringForUser",
            ContentResolver::class.java, String::class.java, Int::class.java
        ) { param ->
            if (param.args[1] == Secure.ANDROID_ID) {
                param.result = config.androidId
            }
        }


        Settings.System::class.java.beforeHookedMethod(
            methodName = "getStringForUser",
            ContentResolver::class.java, String::class.java, Int::class.java
        ) { param ->
            if (param.args[1] == Settings.System.ANDROID_ID) {
                param.result = config.androidId
            }
        }

    }

    private fun hookImei() {
        TelephonyManager::class.java.run {
            setAllMethodResult("getImei", config.imei)
            setMethodResult("getPrimaryImei", config.imei)
            setAllMethodResult("getDeviceId", config.imei)
            setMethodResult("getTypeAllocationCode", config.imei.take(8))
        }
    }

    private fun hookPhoneNum() {
        TelephonyManager::class.java.setMethodResult("getLine1Number", config.phoneNum)
        SubscriptionManager::class.java.setAllMethodResult("getPhoneNumber", config.phoneNum)
        SubscriptionInfo::class.java.setMethodResult("getNumber", config.phoneNum)
    }

}
