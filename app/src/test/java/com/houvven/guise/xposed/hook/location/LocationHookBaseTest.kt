package com.houvven.guise.xposed.hook.location

import android.telephony.PhoneStateListener
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class LocationHookBaseTest {

    @Test
    fun removesOnlyLegacyCellLocationEvents() {
        val retainedEvent = PhoneStateListener.LISTEN_CALL_STATE
        val events = retainedEvent or
            PhoneStateListener.LISTEN_CELL_LOCATION or
            PhoneStateListener.LISTEN_CELL_INFO

        assertEquals(retainedEvent, withoutCellLocationEvents(events))
    }

    @Test
    fun cellOnlyRegistrationBecomesListenNone() {
        val events = PhoneStateListener.LISTEN_CELL_LOCATION or PhoneStateListener.LISTEN_CELL_INFO

        assertEquals(PhoneStateListener.LISTEN_NONE, withoutCellLocationEvents(events))
    }
}
