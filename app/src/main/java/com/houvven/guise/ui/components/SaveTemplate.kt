package com.houvven.guise.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.xposed.config.ModuleConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateTypeSelector(
    type: Int,
    onTypeChanged: (Int) -> Unit,
) {
    val options = listOf(
        Template.Type.COMMON to stringResource(R.string.template_type_common),
        Template.Type.EXCLUSIVE to stringResource(R.string.template_type_app),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            val isExclusive = value == Template.Type.EXCLUSIVE
            val activeContainerColor = if (isExclusive) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            }
            val activeContentColor = if (isExclusive) {
                MaterialTheme.colorScheme.onTertiary
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
            SegmentedButton(
                selected = type == value,
                onClick = { onTypeChanged(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = activeContainerColor,
                    activeContentColor = activeContentColor,
                    activeBorderColor = activeContainerColor,
                ),
                icon = {},
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTemplate(
    dialogState: MutableState<Boolean>,
    moduleConfig: ModuleConfig,
    onSaved: () -> Unit = {},
) {

    if (dialogState.value.not()) return

    var type by remember { mutableIntStateOf(Template.Type.COMMON) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var packageName by remember { mutableStateOf(moduleConfig.packageName) }

    AlertDialog(
        onDismissRequest = { dialogState.value = false },
        title = {
            Text(
                text = stringResource(R.string.save_template),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                TemplateTypeSelector(type = type, onTypeChanged = { type = it })
                if (type == Template.Type.EXCLUSIVE && moduleConfig.packageName.isBlank()) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.package_name)) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && name.isNotBlank()) nameError = false
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.name)) },
                    shape = RoundedCornerShape(10.dp),
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text(
                                text = stringResource(R.string.template_name_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.height(150.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                    return@TextButton
                }
                Template(
                    name = name,
                    description = description,
                    type = type,
                    configuration = moduleConfig.toJson()
                ).also {
                    if (packageName.isNotBlank()) it.packageName = packageName
                }.let {
                    LauncherState.addTemplate(it)
                }

                dialogState.value = false
                onSaved()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dialogState.value = false
            }) { Text(stringResource(R.string.cancel)) }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveEditTemplate(
    dialogState: MutableState<Boolean>,
    template: Template,
    moduleConfig: ModuleConfig,
    prepareConfigForSave: () -> Unit = {},
    onSaved: () -> Unit = {},
) {

    if (dialogState.value.not()) return

    var type by remember { mutableIntStateOf(template.type) }
    var name by remember { mutableStateOf(template.name) }
    var description by remember { mutableStateOf(template.description ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var packageName by remember { mutableStateOf(template.packageName ?: "") }

    AlertDialog(
        onDismissRequest = { dialogState.value = false },
        title = {
            Text(
                text = stringResource(R.string.save_template),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                TemplateTypeSelector(type = type, onTypeChanged = { type = it })
                if (type == Template.Type.EXCLUSIVE && template.packageName.isNullOrBlank()) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.package_name)) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && name.isNotBlank()) nameError = false
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.name)) },
                    shape = RoundedCornerShape(10.dp),
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text(
                                text = stringResource(R.string.template_name_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.height(150.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                    return@TextButton
                }
                prepareConfigForSave()
                LauncherState.updateTemplate(template.also {
                    it.name = name
                    it.description = description
                    it.type = type
                    if (packageName.isNotBlank()) it.packageName = packageName
                    it.configuration = moduleConfig.toJson()
                })

                dialogState.value = false
                GlobalSnackbarHost.showSuccess()
                onSaved()
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dialogState.value = false
            }) { Text(stringResource(R.string.cancel)) }
        }
    )
}
