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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val mutableSyncState = MutableStateFlow(RuntimeLogSyncState())

    private lateinit var localPreferences: SharedPreferences
    private lateinit var dao: RuntimeLogDao
    private var remotePreferences: SharedPreferences? = null
    private var remoteListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    val syncState: StateFlow<RuntimeLogSyncState> = mutableSyncState.asStateFlow()

    val logs: Flow<List<RuntimeLog>>
        get() = dao.observeAll()

    fun initialize(context: Context) {
        if (::dao.isInitialized) return
        val appContext = context.applicationContext
        localPreferences = appContext.getSharedPreferences(LOCAL_PREFERENCES, Context.MODE_PRIVATE)
        dao = RuntimeLogDatabase.create(appContext).runtimeLogDao()
        scope.launch { RuntimeLogDatabase.deleteLegacyDatabase(appContext) }
    }

    fun bind(service: XposedService) {
        unbind()
        runCatching {
            service.getRemotePreferences(RuntimeLogProtocol.PREFERENCES_NAME)
        }.onSuccess { preferences ->
            remotePreferences = preferences
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == null ||
                    key == RuntimeLogProtocol.CLEARED_BEFORE_KEY ||
                    key.startsWith(RuntimeLogProtocol.INBOX_KEY_PREFIX)
                ) {
                    requestSync()
                }
            }
            remoteListener = listener
            preferences.registerOnSharedPreferenceChangeListener(listener)
            mutableSyncState.value = mutableSyncState.value.copy(connected = true, error = null)
            applyLocalSettings(preferences)
            requestSync()
        }.onFailure { error ->
            mutableSyncState.value = RuntimeLogSyncState(connected = false, error = error)
        }
    }

    fun unbind() {
        val preferences = remotePreferences
        val listener = remoteListener
        if (preferences != null && listener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
        remotePreferences = null
        remoteListener = null
        mutableSyncState.value = mutableSyncState.value.copy(connected = false, syncing = false)
    }

    fun requestSync() {
        scope.launch { syncNow() }
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
            remotePreferences?.let { preferences ->
                preferences.edit {
                    putLong(RuntimeLogProtocol.CLEARED_BEFORE_KEY, clearedBefore)
                    preferences.all.keys
                        .filter { it.startsWith(RuntimeLogProtocol.INBOX_KEY_PREFIX) }
                        .forEach(::remove)
                }
            }
            dao.clearAll()
        }
    }

    private suspend fun syncNow() = syncMutex.withLock {
        val preferences = remotePreferences ?: return@withLock
        mutableSyncState.value = mutableSyncState.value.copy(syncing = true, error = null)
        runCatching {
            val localClearedBefore = localPreferences.getLong(LOCAL_CLEARED_BEFORE, 0L)
            val remoteClearedBefore = preferences.getLong(
                RuntimeLogProtocol.CLEARED_BEFORE_KEY,
                0L,
            )
            val clearedBefore = maxOf(localClearedBefore, remoteClearedBefore)
            val logs = preferences.all.asSequence()
                .filter { (key, value) ->
                    key.startsWith(RuntimeLogProtocol.INBOX_KEY_PREFIX) && value is String
                }
                .flatMap { (_, value) -> RuntimeLogProtocol.decode(value as String).asSequence() }
                .filter { it.timestamp > clearedBefore }
                .distinctBy { it.id }
                .map(RuntimeLog::fromEvent)
                .toList()
            if (logs.isNotEmpty()) dao.insertAll(logs)
            dao.trimTo(MAX_STORED_LOGS)
            mutableSyncState.value = RuntimeLogSyncState(
                connected = true,
                lastSyncTime = System.currentTimeMillis(),
            )
        }.onFailure { error ->
            mutableSyncState.value = RuntimeLogSyncState(
                connected = true,
                error = error,
            )
        }
    }

    private fun applyLocalSettings(preferences: SharedPreferences) {
        val clearedBefore = maxOf(
            localPreferences.getLong(LOCAL_CLEARED_BEFORE, 0L),
            preferences.getLong(RuntimeLogProtocol.CLEARED_BEFORE_KEY, 0L),
        )
        val detailedLogging = isDetailedLoggingEnabled()
        localPreferences.edit { putLong(LOCAL_CLEARED_BEFORE, clearedBefore) }
        preferences.edit {
            putLong(RuntimeLogProtocol.CLEARED_BEFORE_KEY, clearedBefore)
            putBoolean(RuntimeLogProtocol.DETAILED_LOGGING_KEY, detailedLogging)
        }
    }
}
