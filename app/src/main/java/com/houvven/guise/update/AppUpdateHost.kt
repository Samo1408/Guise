package com.houvven.guise.update

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.houvven.guise.BuildConfig
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.ui.GlobalSnackbarHost
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
    var detectedUpdate by mutableStateOf<UpdateInfo?>(null)
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
                        detectedUpdate = info
                        availableUpdate = info
                    }
                } else if (reportResult) {
                    detectedUpdate = null
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
        detectedUpdate = null
        availableUpdate = null
    }
}

@Composable
fun AppUpdateHost() {
    LaunchedEffect(Unit) { AppUpdateManager.startStartupCheck() }
    val info = AppUpdateManager.availableUpdate ?: return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val downloadFailedMessage = stringResource(R.string.update_download_failed)
    val updater = remember { AppUpdater() }
    var downloadId by remember(info.versionCode) { mutableStateOf<Long?>(null) }
    var readyDownloadId by remember(info.versionCode) { mutableStateOf<Long?>(null) }
    var progress by remember(info.versionCode) { mutableStateOf<Float?>(null) }
    var error by remember(info.versionCode) { mutableStateOf<String?>(null) }

    LaunchedEffect(info.versionCode) {
        UpdateInstaller.pending(context)
            .takeIf { it >= 0L && UpdateInstaller.isSuccessful(context, it) }
            ?.let { readyDownloadId = it }
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        while (true) {
            val state = updater.downloadProgress(context, id)
            if (state == null) {
                val nextDownloadId = updater.retryNextDownload(context, id)
                downloadId = nextDownloadId
                progress = null
                if (nextDownloadId == null) error = downloadFailedMessage
                return@LaunchedEffect
            }
            progress = state.fraction
            when {
                state.active -> delay(400L)
                state.successful -> {
                    val verified = UpdateInstaller.isVerified(
                        context,
                        id,
                        updater.expectedSha256(context),
                    )
                    if (!verified) {
                        val nextDownloadId = updater.retryNextDownload(context, id)
                        downloadId = nextDownloadId
                        progress = null
                        if (nextDownloadId == null) error = downloadFailedMessage
                        return@LaunchedEffect
                    }
                    downloadId = null
                    progress = null
                    readyDownloadId = id
                    UpdateInstaller.markReady(context, id)
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        val installedWithRoot = UpdateInstaller.silentInstallWithRoot(context, id)
                        if (!installedWithRoot) {
                            val launched = (context as? Activity)?.let { activity ->
                                UpdateInstaller.launchInstaller(activity, id)
                            } ?: false
                            if (!launched) UpdateInstallActivity.open(context, id)
                        }
                    }
                    AppUpdateManager.dismiss()
                    return@LaunchedEffect
                }
                else -> {
                    val nextDownloadId = updater.retryNextDownload(context, id)
                    downloadId = nextDownloadId
                    progress = null
                    if (nextDownloadId == null) error = downloadFailedMessage
                    return@LaunchedEffect
                }
            }
        }
    }
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
                    UpdateNotesText(info.notes)
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = downloadId == null,
                onClick = {
                    error = null
                    val readyId = readyDownloadId
                    if (readyId != null) {
                        UpdateInstallActivity.open(context, readyId)
                    } else {
                        runCatching { updater.download(context, info) }
                            .onSuccess { downloadId = it }
                            .onFailure {
                                error = it.message ?: downloadFailedMessage
                            }
                    }
                },
            ) {
                if (downloadId != null) {
                    val currentProgress = progress
                    if (currentProgress != null) {
                        CircularProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.update_downloading))
                } else {
                    Text(
                        stringResource(
                            if (readyDownloadId != null) {
                                R.string.update_install
                            } else {
                                R.string.update_action
                            },
                        ),
                    )
                }
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
