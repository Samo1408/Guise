package com.houvven.guise.ui.routing.editor

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.houvven.guise.R
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.components.SaveTemplate
import com.houvven.guise.ui.components.simplify.SimplifyDropdownMenuItem
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.ui.theme.predictiveBack
import com.houvven.guise.ui.utils.oneClickRandom
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.guise.xposed.config.ModuleConfigManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private enum class EditorExitDialog {
    NONE,
    SAVE_CHANGES,
    APPLY_CHANGES,
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DeployConfigEditScreen(name: String, packageName: String) {
    val context = LocalContext.current
    val navHostController = LocalNavController.current
    val moduleConfigManager = remember(packageName) {
        ModuleConfigManager.of(ModuleConfig.get(packageName))
    }
    val isSaveRequest = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val resetPendingMessage = stringResource(R.string.config_reset_pending_save)
    var exitDialog by rememberSaveable { mutableStateOf(EditorExitDialog.NONE) }
    var savedChangesNeedApply by rememberSaveable { mutableStateOf(false) }
    var savedSnapshotGeneration by remember { mutableIntStateOf(0) }
    val hasUnsavedChanges by remember(moduleConfigManager, savedSnapshotGeneration) {
        derivedStateOf(moduleConfigManager::hasUnsavedChanges)
    }
    val requiresExitDecision = hasUnsavedChanges || savedChangesNeedApply

    fun leaveEditor() {
        exitDialog = EditorExitDialog.NONE
        navHostController.popBackStack()
    }

    fun requestApplyChoice() {
        val hasRunningHookedTarget = moduleConfigManager.hasRunningHookedTarget()
        if (moduleConfigManager.hasNoConfiguredParameters() &&
            moduleConfigManager.config.enabled
        ) {
            moduleConfigManager.setEnabled(false)
        }
        if (hasRunningHookedTarget == false) {
            leaveEditor()
        } else {
            exitDialog = EditorExitDialog.APPLY_CHANGES
        }
    }

    fun requestExit() {
        when {
            hasUnsavedChanges ->
                exitDialog = EditorExitDialog.SAVE_CHANGES

            savedChangesNeedApply -> requestApplyChoice()
            else -> leaveEditor()
        }
    }

    fun saveChanges() {
        val changed = moduleConfigManager.hasUnsavedChanges()
        moduleConfigManager.save()
        savedSnapshotGeneration++
        if (changed) savedChangesNeedApply = true
    }

    fun runProcessAction(action: suspend () -> Result<Unit>) {
        exitDialog = EditorExitDialog.NONE
        coroutineScope.launch {
            action().onFailure {
                GlobalSnackbarHost.showOnErrorByDismissPrevious(
                    it.message ?: it.toString()
                )
            }
            navHostController.popBackStack()
        }
    }

    BackHandler(enabled = !predictiveBack.value) { requestExit() }
    PredictiveBackHandler(
        enabled = predictiveBack.value && requiresExitDecision,
    ) { progress ->
        progress.collect()
        requestExit()
    }

    SaveTemplate(isSaveRequest, moduleConfigManager.config)

    ConfigEditorView(moduleConfigManager.state) {
        TopAppBar(
            title = { Text(name, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                IconButton(onClick = ::requestExit) {
                    SimplifyIcon(Icons.AutoMirrored.Filled.ArrowBack)
                }
            },
            actions = {
                IconButton({
                    moduleConfigManager.clear()
                    GlobalSnackbarHost.showByDismissPrevious(resetPendingMessage)
                }) { SimplifyIcon(Icons.Outlined.Delete) }

                IconButton(
                    onClick = {
                        saveChanges()
                        GlobalSnackbarHost.showSuccess()
                    },
                    enabled = hasUnsavedChanges,
                ) { SimplifyIcon(Icons.Outlined.Save) }

                var expanded by remember { mutableStateOf(false) }
                IconButton({ expanded = true }) {
                    SimplifyIcon(Icons.Rounded.MoreVert)
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SimplifyDropdownMenuItem(
                            stringResource(R.string.one_click_random),
                            onClick = {
                                expanded = false
                                oneClickRandom(moduleConfigManager.state, context)
                            }
                        )
                        SimplifyDropdownMenuItem(
                            text = stringResource(R.string.save_as_template),
                            onClick = {
                                expanded = false
                                isSaveRequest.value = true
                            }
                        )
                    }
                }
            }
        )
    }

    if (exitDialog == EditorExitDialog.SAVE_CHANGES) {
        AlertDialog(
            onDismissRequest = { exitDialog = EditorExitDialog.NONE },
            title = { Text(stringResource(R.string.unsaved_config_title)) },
            text = { Text(stringResource(R.string.unsaved_config_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    saveChanges()
                    requestApplyChoice()
                }) {
                    Text(stringResource(R.string.save_changes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (savedChangesNeedApply) requestApplyChoice() else leaveEditor()
                }) {
                    Text(stringResource(R.string.discard_changes))
                }
            },
        )
    }

    if (exitDialog == EditorExitDialog.APPLY_CHANGES) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.apply_config_title)) },
            text = { Text(stringResource(R.string.apply_config_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    runProcessAction { moduleConfigManager.restartApp() }
                }) {
                    Text(stringResource(R.string.restart_app))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = ::leaveEditor) {
                        Text(stringResource(R.string.ignore))
                    }
                    TextButton(onClick = {
                        runProcessAction { moduleConfigManager.stopApp() }
                    }) {
                        Text(stringResource(R.string.stop_app))
                    }
                }
            },
        )
    }
}
