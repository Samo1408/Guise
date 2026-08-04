package com.houvven.guise.ui.routing.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.houvven.guise.db.Template
import com.houvven.guise.ui.components.SaveEditTemplate
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.guise.xposed.config.ModuleConfigManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateScreen(template: Template) {
    val navHostController = LocalNavController.current
    val moduleConfigManager = remember(template.id, template.configuration) {
        ModuleConfigManager.of(ModuleConfig.fromJson(template.configuration))
    }
    val isSaveRequest = remember { mutableStateOf(false) }
    val hasParameterChanges = moduleConfigManager.hasUnsavedChanges()

    SaveEditTemplate(
        dialogState = isSaveRequest,
        template = template,
        moduleConfig = moduleConfigManager.config,
        prepareConfigForSave = moduleConfigManager::updateConfigFromState,
        onSaved = { navHostController.popBackStack() },
    )

    ConfigEditorView(moduleConfigManager.state) {
        TopAppBar(
            title = {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = { navHostController.popBackStack() }) {
                    SimplifyIcon(Icons.AutoMirrored.Filled.ArrowBack)
                }
            },
            actions = {
                androidx.compose.material3.IconButton(onClick = { moduleConfigManager.clear() }) {
                    SimplifyIcon(Icons.Default.Delete)
                }
                androidx.compose.material3.IconButton(
                    enabled = hasParameterChanges,
                    onClick = { isSaveRequest.value = true },
                ) {
                    SimplifyIcon(Icons.Default.Save)
                }
            }
        )
    }
}
