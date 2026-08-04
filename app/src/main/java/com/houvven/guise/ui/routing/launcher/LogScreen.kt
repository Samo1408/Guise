package com.houvven.guise.ui.routing.launcher

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.houvven.guise.BuildConfig
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.log.RuntimeLog
import com.houvven.guise.log.RuntimeLogStore
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.utils.saveFileToDownloadDir
import com.houvven.ktx_xposed.logger.XposedLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LogFilter {
    ALL,
    ERROR,
    INFO,
    DEBUG,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val logs by RuntimeLogStore.logs.collectAsStateWithLifecycle(initialValue = emptyList())
    val syncState by RuntimeLogStore.syncState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var detailedLogging by rememberSaveable {
        mutableStateOf(RuntimeLogStore.isDetailedLoggingEnabled())
    }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { RuntimeLogStore.requestSync() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { RuntimeLogStore.requestSync() }

    val visibleLogs = remember(logs, filter, query) {
        logs.filter { log ->
            val matchesLevel = when (filter) {
                LogFilter.ALL -> true
                LogFilter.ERROR -> log.level == XposedLogger.Level.ERROR
                LogFilter.INFO -> log.level == XposedLogger.Level.INFO
                LogFilter.DEBUG -> log.level == XposedLogger.Level.DEBUG
            }
            val matchesQuery = query.isBlank() || listOf(
                log.packageName,
                log.processName,
                log.category,
                log.message,
                log.stackTrace,
            ).any { it.contains(query, ignoreCase = true) }
            matchesLevel && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_log)) },
                actions = {
                    IconButton(
                        onClick = { confirmClear = true },
                        enabled = logs.isNotEmpty(),
                    ) {
                        SimplifyIcon(Icons.Outlined.Delete)
                    }
                    IconButton(
                        onClick = {
                            val snapshot = logs.toList()
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    val content = buildDiagnosticLog(context, snapshot)
                                    saveFileToDownloadDir(
                                        "Guise-log-${System.currentTimeMillis()}.log",
                                        content,
                                    )
                                }
                                result.onSuccess {
                                    GlobalSnackbarHost.showByDismissPrevious(
                                        resources.getString(R.string.save_success, it)
                                    )
                                }.onFailure {
                                    GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                        resources.getString(
                                            R.string.save_failed,
                                            it.message.orEmpty(),
                                        )
                                    )
                                }
                            }
                        },
                        enabled = logs.isNotEmpty(),
                    ) {
                        SimplifyIcon(Icons.Outlined.Save)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize(),
        ) {
            if (syncState.syncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            DetailedLoggingControl(
                enabled = detailedLogging,
                connected = syncState.connected,
                onEnabledChange = { enabled ->
                    detailedLogging = enabled
                    RuntimeLogStore.setDetailedLogging(enabled)
                },
            )

            if (logs.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    leadingIcon = { SimplifyIcon(Icons.Outlined.Search) },
                    placeholder = { Text(stringResource(R.string.log_search_hint)) },
                    singleLine = true,
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        LogFilterChip(
                            selected = filter == LogFilter.ALL,
                            text = stringResource(R.string.log_filter_all),
                            onClick = { filter = LogFilter.ALL },
                        )
                    }
                    item {
                        LogFilterChip(
                            selected = filter == LogFilter.ERROR,
                            text = stringResource(R.string.log_filter_error),
                            onClick = { filter = LogFilter.ERROR },
                        )
                    }
                    item {
                        LogFilterChip(
                            selected = filter == LogFilter.INFO,
                            text = stringResource(R.string.log_filter_info),
                            onClick = { filter = LogFilter.INFO },
                        )
                    }
                    item {
                        LogFilterChip(
                            selected = filter == LogFilter.DEBUG,
                            text = stringResource(R.string.log_filter_debug),
                            onClick = { filter = LogFilter.DEBUG },
                        )
                    }
                }
            }

            if (visibleLogs.isEmpty()) {
                LogEmptyState(hasLogs = logs.isNotEmpty(), modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 4.dp,
                        end = 12.dp,
                        bottom = paddingValues.calculateBottomPadding() + 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleLogs, key = RuntimeLog::id) { log ->
                        RuntimeLogCard(log)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_logs_title)) },
            text = { Text(stringResource(R.string.clear_logs_message)) },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    RuntimeLogStore.clear()
                }) {
                    Text(stringResource(R.string.clear))
                }
            },
        )
    }
}

@Composable
private fun DetailedLoggingControl(
    enabled: Boolean,
    connected: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = connected) { onEnabledChange(!enabled) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.detailed_logging),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        if (connected) {
                            R.string.detailed_logging_description
                        } else {
                            R.string.detailed_logging_unavailable
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = connected,
            )
        }
    }
}

@Composable
private fun LogFilterChip(selected: Boolean, text: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
    )
}

@Composable
private fun LogEmptyState(hasLogs: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SimplifyIcon(
                imageVector = Icons.Outlined.Description,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (hasLogs) R.string.no_matching_logs else R.string.no_logs_found
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            if (!hasLogs) {
                Text(
                    text = stringResource(R.string.runtime_log_recording_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RuntimeLogCard(log: RuntimeLog) {
    var expanded by rememberSaveable(log.id) { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()) }
    val levelColor = when (log.level) {
        XposedLogger.Level.ERROR -> MaterialTheme.colorScheme.error
        XposedLogger.Level.DEBUG -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val levelLabel = when (log.level) {
        XposedLogger.Level.ERROR -> stringResource(R.string.log_filter_error)
        XposedLogger.Level.DEBUG -> stringResource(R.string.log_filter_debug)
        else -> stringResource(R.string.log_filter_info)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.packageName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (log.processName != log.packageName) {
                        Text(
                            text = log.processName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = levelLabel,
                    color = levelColor,
                    style = MaterialTheme.typography.labelMedium,
                )
                SimplifyIcon(
                    imageVector = if (expanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${timeFormatter.format(Date(log.timestamp))} · ${log.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AnimatedVisibility(visible = expanded && log.stackTrace.isNotBlank()) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = log.stackTrace,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun buildDiagnosticLog(context: android.content.Context, logs: List<RuntimeLog>): String {
    val service = ContextAmbient.xposedService
    val packageManager = context.packageManager
    val appPackage = packageManager.getPackageInfo(context.packageName, 0)
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ROOT)
    return buildString {
        appendLine("Guise runtime diagnostic log")
        appendLine("Generated: ${formatter.format(Date())}")
        appendLine("Guise: ${BuildConfig.VERSION_NAME} (${appPackage.longVersionCode})")
        appendLine(
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}); " +
                "${Build.MANUFACTURER} ${Build.MODEL}"
        )
        appendLine(
            "Xposed: ${service?.frameworkName ?: "unavailable"} " +
                "${service?.frameworkVersion.orEmpty()} (API ${service?.apiVersion ?: "unavailable"})"
        )
        logs.asSequence().map(RuntimeLog::packageName).distinct().sorted().forEach { packageName ->
            val version = runCatching { packageManager.getPackageInfo(packageName, 0) }
                .map { "${it.versionName.orEmpty()} (${it.longVersionCode})" }
                .getOrDefault("unavailable")
            appendLine("Target: $packageName $version")
        }
        appendLine("Entries: ${logs.size}")
        appendLine()
        logs.asReversed().forEach { log ->
            append(formatter.format(Date(log.timestamp)))
            append(" [${log.level}] ")
            append(log.packageName)
            append("/")
            append(log.processName)
            append(" ")
            append(log.category)
            append(": ")
            appendLine(log.message)
            if (log.stackTrace.isNotBlank()) appendLine(log.stackTrace)
        }
    }
}
