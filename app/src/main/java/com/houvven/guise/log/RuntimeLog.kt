package com.houvven.guise.log

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.houvven.ktx_xposed.logger.RuntimeLogEvent

@Entity(
    tableName = "runtime_log",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["level"]),
        Index(value = ["packageName"]),
    ],
)
data class RuntimeLog(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val level: Char,
    val packageName: String,
    val processName: String,
    val category: String,
    val message: String,
    val stackTrace: String,
) {
    companion object {
        fun fromEvent(event: RuntimeLogEvent) = RuntimeLog(
            id = event.id,
            timestamp = event.timestamp,
            level = event.level,
            packageName = event.packageName,
            processName = event.processName,
            category = event.category,
            message = event.message,
            stackTrace = event.stackTrace,
        )
    }
}
