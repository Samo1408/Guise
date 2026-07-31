package com.houvven.guise.ui.routing.template

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.guise.xposed.config.ModuleConfigManager

@Composable
fun EnableTemplateDialog(state: MutableState<Boolean>, template: Template) {
    if (!state.value) return
    AlertDialog(
        title = { Text(text = stringResource(R.string.notice)) },
        text = { Text(text = stringResource(R.string.enable_template_question)) },
        onDismissRequest = { state.value = false },
        confirmButton = {
            TextButton(onClick = {
                val packageName = requireNotNull(template.packageName)
                val config = ModuleConfig.fromJson(template.configuration)
                    .copy(packageName = packageName, enabled = true)
                ModuleConfigManager.of(config).setEnabled(true)
                state.value = false
                GlobalSnackbarHost.showSuccess()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { state.value = false }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

}
