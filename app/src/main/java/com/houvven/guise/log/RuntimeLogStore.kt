package com.houvven.guise.log

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.houvven.ktx_xposed.logger.RuntimeLogProtocol
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class RuntimeLogSyncState(
    val connected: Boolean = false,
    val syncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: Throwable? = null,
)

object RuntimeLogStore {
    private const val MAX_STORED_LOGS = 2_000
    private const val LOCAL_PREFERENCES = "runtime_log_state_v2"
    private const val LOCAL_CLEARED_BEFORE = "cleared_before"
    private const val LOCAL_DELIVERY_TOKEN = "delivery_token"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableSyncState = MutableStateFlow(RuntimeLogSyncState())

    private lateinit var localPreferences: SharedPreferences
    private lateinit var dao: RuntimeLogDao
    private var remotePreferences: SharedPreferences? = null

    val syncState: StateFlow<RuntimeLogSyncState> = mutableSyncState.asStateFlow()

    val logs: Flow<List<RuntimeLog>>
        get() = dao.observeAll()

    @Synchronized
    fun initialize(context: Context) {
        if (::dao.isInitialized) return
        val appContext = context.applicationContext
        localPreferences = appContext.getSharedPreferences(LOCAL_PREFERENCES, Context.MODE_PRIVATE)
        if (!localPreferences.contains(LOCAL_DELIVERY_TOKEN)) {
            localPreferences.edit(commit = true) {
                putString(LOCAL_DELIVERY_TOKEN, UUID.randomUUID().toString())
            }
        }
        dao = RuntimeLogDatabase.create(appContext).runtimeLogDao()
        scope.launch { RuntimeLogDatabase.deleteLegacyDatabase(appContext) }
    }

    fun bind(service: XposedService) {
        unbind()
        runCatching {
            service.getRemotePreferences(RuntimeLogProtocol.PREFERENCES_NAME)
        }.onSuccess { preferences ->
            remotePreferences = preferences
            applyLocalSettings(preferences)
            mutableSyncState.value = RuntimeLogSyncState(
                connected = true,
                lastSyncTime = System.currentTimeMillis(),
            )
        }.onFailure { error ->
            mutableSyncState.value = RuntimeLogSyncState(connected = false, error = error)
        }
    }

    fun unbind() {
        remotePreferences = null
        mutableSyncState.value = mutableSyncState.value.copy(connected = false, syncing = false)
    }

    fun requestSync() {
        mutableSyncState.value = mutableSyncState.value.copy(
            lastSyncTime = System.currentTimeMillis(),
            error = null,
        )
    }

    fun setDetailedLogging(enabled: Boolean) {
        localPreferences.edit {
            putBoolean(RuntimeLogProtocol.DETAILED_LOGGING_KEY, enabled)
        }
        remotePreferences?.edit {
            putBoolean(RuntimeLogProtocol.DETAILED_LOGGING_KEY, enabled)
        }
    }

    fun isDetailedLoggingEnabled(): Boolean = localPreferences.getBoolean(
        RuntimeLogProtocol.DETAILED_LOGGING_KEY,
        false,
    )

    fun clear() {
        scope.launch {
            val clearedBefore = System.currentTimeMillis()
            localPreferences.edit { putLong(LOCAL_CLEARED_BEFORE, clearedBefore) }
            dao.clearAll()
        }
    }

    suspend fun append(events: List<com.houvven.ktx_xposed.logger.RuntimeLogEvent>) {
        val clearedBefore = localPreferences.getLong(LOCAL_CLEARED_BEFORE, 0L)
        val logs = events.asSequence()
            .filter { it.timestamp > clearedBefore }
            .distinctBy { it.id }
            .map(RuntimeLog::fromEvent)
            .toList()
        if (logs.isNotEmpty()) dao.insertAll(logs)
        dao.trimTo(MAX_STORED_LOGS)
    }

    fun appendAsync(
        events: List<com.houvven.ktx_xposed.logger.RuntimeLogEvent>,
        onComplete: () -> Unit,
    ) {
        scope.launch {
            try {
                append(events)
            } finally {
                onComplete()
            }
        }
    }

    fun deliveryToken(): String? = localPreferences.getString(LOCAL_DELIVERY_TOKEN, null)

    private fun applyLocalSettings(preferences: SharedPreferences) {
        val detailedLogging = isDetailedLoggingEnabled()
        preferences.edit {
            putBoolean(RuntimeLogProtocol.DETAILED_LOGGING_KEY, detailedLogging)
            putString(RuntimeLogProtocol.DELIVERY_TOKEN_KEY, deliveryToken())
        }
    }
}
