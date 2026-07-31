package com.houvven.guise.ui.routing.editor


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.List
import androidx.compose.material.icons.twotone.Casino
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houvven.guise.R
import com.houvven.guise.ui.components.ElevatedTextField

private val FieldActionButtonSize = 32.dp
private val FieldActionIconSize = 22.dp
private val FieldActionsWidth = FieldActionButtonSize * 3

@Composable
internal fun Title(
    text: String,
    topPadding: Dp = 22.dp,
    supportingText: String? = null,
) {
    ConfigItem(supportingText) {
        Text(
            text = text,
            modifier = Modifier.padding(top = topPadding, start = 25.dp, bottom = 5.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
/**
 * 自定义容器 默认为水平排列
 */
@Composable
internal fun Container(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 15.dp, vertical = 3.dp)
            .fillMaxSize(),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 3.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            content()
        }
    }
}

@Composable
internal fun ContainerSwitch(
    state: MutableState<Boolean>,
    label: String,
    supportingText: String? = null,
) {
    ConfigItem(supportingText) {
        Container {
            Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = state.value, onCheckedChange = { state.value = it })
        }
    }
}

@Composable
private fun ConfigItem(
    supportingText: String?,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
        supportingText?.let { text ->
            Text(
                text = text,
                modifier = Modifier.padding(start = 31.dp, top = 1.dp, end = 31.dp, bottom = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FieldIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    clickable: () -> Unit,
) {
    IconButton(onClick = clickable, modifier = Modifier.size(FieldActionButtonSize)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(FieldActionIconSize),
            tint = tint,
        )
    }
}

@Composable
private fun FieldActions(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.width(FieldActionsWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        content()
    }
}

@Composable
private fun BasicInputBox(
    state: MutableState<String>,
    label: String,
    supportingText: String? = null,
    validate: (String) -> Boolean = { true },
    trailingIcon: @Composable () -> Unit = {},
) {
    val modifier = Modifier
        .padding(horizontal = 15.dp, vertical = 3.dp)
        .fillMaxWidth()
    ConfigItem(supportingText) {
        ElevatedTextField(
            value = state.value,
            onValueChange = { state.value = if (validate(it)) it else state.value },
            modifier = modifier,
            singleLine = true,
            label = { Text(text = label) },
            trailingIcon = trailingIcon,
        )
    }
}


@Composable
internal fun InputBox(
    state: MutableState<String>,
    label: String,
    supportingText: String? = null,
    validate: (String) -> Boolean = { true },
) {
    BasicInputBox(
        state = state,
        label = label,
        supportingText = supportingText,
        validate = validate,
    ) {
        FieldActions {
            if (state.value.isNotBlank()) FieldIconButton(
                Icons.TwoTone.Delete,
                stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.primary,
            ) {
                state.value = ""
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun OperateInputBox(
    state: MutableState<String>,
    label: String,
    supportingText: String? = null,
    showOperateIcon: Boolean = true,
    validate: (String) -> Boolean = { true },
    operateIcon: ImageVector = Icons.AutoMirrored.TwoTone.List,
    operateContentDescription: String = stringResource(R.string.choose_preset),
    secondaryAction: InputFieldAction? = null,
    clickable: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val initialValue = remember(state) { state.value }
    val hasConfiguredValueOrChange = state.value.isNotBlank() || state.value != initialValue
    val operateIconTint = if (hasConfiguredValueOrChange) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    fun runAction(action: () -> Unit) {
        focusManager.clearFocus()
        keyboardController?.hide()
        action()
    }

    BasicInputBox(state, label, supportingText, validate) {
        FieldActions {
            if (showOperateIcon) {
                FieldIconButton(
                    operateIcon,
                    operateContentDescription,
                    tint = operateIconTint,
                    clickable = { runAction(clickable) },
                )
            }
            secondaryAction?.let { action ->
                FieldIconButton(
                    action.icon,
                    action.contentDescription,
                    tint = operateIconTint,
                    clickable = { runAction(action.onClick) },
                )
            }
            if (state.value.isNotBlank()) {
                FieldIconButton(
                    Icons.TwoTone.Delete,
                    stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    state.value = ""
                }
            }
        }
    }
}

@Composable
internal fun RandomInputBox(
    state: MutableState<String>,
    label: String,
    supportingText: String? = null,
    validate: (String) -> Boolean = { true },
    randomGenerator: () -> String,
) {
    OperateInputBox(
        state = state,
        label = label,
        supportingText = supportingText,
        validate = validate,
        operateIcon = Icons.TwoTone.Casino,
        operateContentDescription = stringResource(R.string.one_click_random),
        clickable = { state.value = randomGenerator() }
    )
}

internal data class InputFieldAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)
