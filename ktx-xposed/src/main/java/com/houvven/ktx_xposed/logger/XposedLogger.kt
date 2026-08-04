package com.houvven.ktx_xposed.logger

import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.houvven.ktx_xposed.hook.ModernXposedRuntime
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object XposedLogger {

    private const val TAG_PREFIX = "Guise"
    private const val DEFAULT_CATEGORY = "Runtime"
    private const val MAX_MESSAGE_LENGTH = 1_024
    private const val MAX_STACK_TRACE_LENGTH = 4_096

    object Level {
        const val DEBUG = 'D'
        const val INFO = 'I'
        const val ERROR = 'E'
    }

    private val sequence = AtomicLong()
    private val inboxFailureReported = AtomicBoolean()
    private val categoryContext = ThreadLocal<String?>()

    val currentCategory: String
        get() = categoryContext.get() ?: DEFAULT_CATEGORY

    fun <T> withCategory(category: String, block: () -> T): T {
        val previous = categoryContext.get()
        categoryContext.set(category)
        return try {
            block()
        } finally {
            if (previous == null) categoryContext.remove() else categoryContext.set(previous)
        }
    }

    fun d(msg: String, category: String = DEFAULT_CATEGORY) {
        basicLog(Level.DEBUG, category, msg, null)
    }

    fun i(msg: String, category: String = DEFAULT_CATEGORY) {
        basicLog(Level.INFO, category, msg, null)
    }

    fun e(msg: String, category: String = DEFAULT_CATEGORY) {
        basicLog(Level.ERROR, category, msg, null)
    }

    fun e(throwable: Throwable, category: String = currentCategory) {
        basicLog(Level.ERROR, category, throwable.toString(), throwable)
    }

    @Synchronized
    private fun basicLog(level: Char, category: String, msg: String, throwable: Throwable?) {
        val module = ModernXposedRuntime.moduleOrNull ?: return
        val context = ModernXposedRuntime.packageContextOrNull ?: return
        val preferences = runCatching {
            module.getRemotePreferences(RuntimeLogProtocol.PREFERENCES_NAME)
        }.getOrNull()
        if (level == Level.DEBUG &&
            preferences?.getBoolean(RuntimeLogProtocol.DETAILED_LOGGING_KEY, false) != true
        ) {
            return
        }
        val priority = when (level) {
            Level.DEBUG -> Log.DEBUG
            Level.ERROR -> Log.ERROR
            else -> Log.INFO
        }
        runCatching {
            if (throwable == null) {
                module.log(priority, "$TAG_PREFIX/$category", msg)
            } else {
                module.log(priority, "$TAG_PREFIX/$category", msg, throwable)
            }
        }

        runCatching {
            checkNotNull(preferences) { "Runtime log preferences are unavailable" }
            val key = RuntimeLogProtocol.inboxKey(context.packageName, context.processName)
            val clearedBefore = preferences.getLong(RuntimeLogProtocol.CLEARED_BEFORE_KEY, 0L)
            val now = System.currentTimeMillis()
            val event = RuntimeLogEvent(
                id = "$now-${Process.myPid()}-${SystemClock.elapsedRealtimeNanos()}-${sequence.incrementAndGet()}",
                timestamp = now,
                level = level,
                packageName = context.packageName,
                processName = context.processName,
                category = category.take(MAX_MESSAGE_LENGTH),
                message = msg.take(MAX_MESSAGE_LENGTH),
                stackTrace = throwable?.fullStackTrace().orEmpty().take(MAX_STACK_TRACE_LENGTH),
            )
            val events = (RuntimeLogProtocol.decode(preferences.getString(key, null)) + event)
                .filter { it.timestamp > clearedBefore }
                .takeLast(RuntimeLogProtocol.MAX_EVENTS_PER_PROCESS)
            check(
                preferences.edit()
                    .putString(key, RuntimeLogProtocol.encode(events))
                    .commit()
            ) { "Runtime log preferences commit failed" }
        }.onFailure { error ->
            if (inboxFailureReported.compareAndSet(false, true)) {
                runCatching {
                    module.log(Log.WARN, "$TAG_PREFIX/Logger", "Unable to persist runtime log inbox", error)
                }
            }
        }
    }

    private fun Throwable.fullStackTrace(): String = StringWriter().also { writer ->
        printStackTrace(PrintWriter(writer))
    }.toString()
}
