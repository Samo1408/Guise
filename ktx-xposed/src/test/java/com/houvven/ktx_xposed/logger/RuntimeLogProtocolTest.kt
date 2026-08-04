package com.houvven.ktx_xposed.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeLogProtocolTest {

    @Test
    fun roundTripPreservesUnicodeAndMultilineContent() {
        val event = RuntimeLogEvent(
            id = "event-1",
            timestamp = 1_785_000_000_123,
            level = 'E',
            packageName = "com.example.测试",
            processName = "com.example.测试:remote",
            category = "LocationHook",
            message = "第一行\nsecond | line",
            stackTrace = "异常\n\tat example.Call.run(Call.kt:1)",
        )

        assertEquals(listOf(event), RuntimeLogProtocol.decode(RuntimeLogProtocol.encode(listOf(event))))
    }

    @Test
    fun malformedEntriesAreIgnoredWithoutDroppingValidEntries() {
        val event = RuntimeLogEvent(
            id = "event-2",
            timestamp = 42,
            level = 'I',
            packageName = "com.example",
            processName = "com.example",
            category = "Runtime",
            message = "ready",
            stackTrace = "",
        )
        val encoded = "broken\n${RuntimeLogProtocol.encode(listOf(event))}\n2|invalid"

        assertEquals(listOf(event), RuntimeLogProtocol.decode(encoded))
        assertTrue(RuntimeLogProtocol.decode(null).isEmpty())
    }
}
