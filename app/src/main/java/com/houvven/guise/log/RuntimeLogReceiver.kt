package com.houvven.guise.log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.houvven.ktx_xposed.logger.RuntimeLogEvent
import com.houvven.ktx_xposed.logger.RuntimeLogProtocol

class RuntimeLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RuntimeLogProtocol.DELIVERY_ACTION) return
        RuntimeLogStore.initialize(context)
        val events = RuntimeLogProtocol.decode(
            intent.getStringExtra(RuntimeLogProtocol.DELIVERY_EVENTS_EXTRA)
        )
        if (events.isEmpty() || !isTrustedSender(context, intent, events)) return

        val pendingResult = goAsync()
        RuntimeLogStore.appendAsync(events) {
            pendingResult.finish()
        }
    }

    private fun isTrustedSender(
        context: Context,
        intent: Intent,
        events: List<RuntimeLogEvent>,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val callerPackages = context.packageManager
                .getPackagesForUid(sentFromUid)
                ?.toSet()
                .orEmpty()
            if (callerPackages.isNotEmpty() && events.all { it.packageName in callerPackages }) {
                return true
            }
        }
        val token = intent.getStringExtra(RuntimeLogProtocol.DELIVERY_TOKEN_EXTRA)
        return token != null && token == RuntimeLogStore.deliveryToken()
    }
}
