package com.houvven.guise.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.UserHandle
import android.widget.Toast

private const val TAG = "AndroidKtx"

/**
 * Check if the package is a system app
 * @return true if the package is a system app
 */
val PackageInfo.isSystemApp: Boolean get() = applicationInfo?.isSystemApp ?: false

/**
 * Check if the application is a system app
 * @return true if the application is a system app
 */
val ApplicationInfo.isSystemApp: Boolean get() = flags and ApplicationInfo.FLAG_SYSTEM != 0

/**
 * Current app run user 's id
 */
val MY_USER_ID = runCatching {
    UserHandle::class.java.getMethod("myUserId").invoke(null) as Int
}.getOrDefault(-1)


fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resId, duration).show()
}

