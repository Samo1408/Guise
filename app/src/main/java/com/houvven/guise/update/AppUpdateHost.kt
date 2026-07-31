package com.houvven.guise.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.houvven.guise.BuildConfig
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.util.android.IntentUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AppUpdateManager {
    private val updater = AppUpdater()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val startupStarted = AtomicBoolean(false)
    private var manualResultRequested = false

    var checking by mutableStateOf(false)
        private set
    var availableUpdate by mutableStateOf<UpdateInfo?>(null)
        private set

    fun check(manual: Boolean) {
        if (checking) {
            manualResultRequested = manualResultRequested || manual
            return
        }
        manualResultRequested = manual
        checking = true
        scope.launch {
            val result = runCatching { updater.check() }
            checking = false
            val reportResult = manualResultRequested
            manualResultRequested = false
            result.onSuccess { info ->
                if (updater.isNewer(info)) {
                    if (reportResult || !UpdatePromptPreferences.isIgnored(ContextAmbient.current, info.versionCode)) {
                        availableUpdate = info
                    }
                } else if (reportResult) {
                    GlobalSnackbarHost.showByDismissPrevious(
                        ContextAmbient.current.getString(
                            R.string.update_already_latest,
                            BuildConfig.VERSION_NAME,
                        ),
                    )
                }
            }.onFailure {
                if (reportResult) {
                    GlobalSnackbarHost.showOnErrorByDismissPrevious(
                        ContextAmbient.current.getString(R.string.update_check_failed),
                    )
                }
            }
        }
    }

    fun startStartupCheck() {
        if (!startupStarted.compareAndSet(false, true)) return
        scope.launch {
            delay(1_500L)
            check(manual = false)
        }
    }

    fun dismiss() {
        availableUpdate = null
    }

    fun ignore(info: UpdateInfo) {
        UpdatePromptPreferences.ignore(ContextAmbient.current, info.versionCode)
        availableUpdate = null
    }
}

@Composable
fun AppUpdateHost() {
    LaunchedEffect(Unit) { AppUpdateManager.startStartupCheck() }
    val info = AppUpdateManager.availableUpdate ?: return
    AlertDialog(
        onDismissRequest = AppUpdateManager::dismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(R.string.update_available_title, info.versionName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (info.title.isNotBlank()) Text(info.title)
                if (info.notes.isNotBlank()) {
                    Text(
                        stringResource(R.string.update_notes),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(info.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppUpdateManager.dismiss()
                IntentUtils.openBrowser(info.actionUrl)
            }) {
                Text(stringResource(R.string.update_go_to_download))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { AppUpdateManager.ignore(info) }) {
                    Text(stringResource(R.string.update_ignore_version))
                }
                TextButton(onClick = AppUpdateManager::dismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        },
    )
}
