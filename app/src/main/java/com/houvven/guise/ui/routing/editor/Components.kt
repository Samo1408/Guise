package com.houvven.guise.ui.routing.editor


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

@Composable
internal fun Title(text: String, topPadding: Dp = 22.dp) {
    Text(
        text = text,
        modifier = Modifier.padding(top = topPadding, start = 25.dp, bottom = 5.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
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
internal fun ContainerSwitch(state: MutableState<Boolean>, label: String) {
    Container {
        Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = state.value, onCheckedChange = { state.value = it })
    }
}

@Composable
private fun FieldIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    clickable: () -> Unit,
) {
    Row {
        IconButton(onClick = clickable, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
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
    val bottomPadding = if (supportingText == null) 3.dp else 9.dp
    val modifier = Modifier
        .padding(start = 15.dp, top = 3.dp, end = 15.dp, bottom = bottomPadding)
        .fillMaxWidth()
    ElevatedTextField(
        value = state.value,
        onValueChange = { state.value = if (validate(it)) it else state.value },
        modifier = modifier,
        singleLine = true,
        label = { Text(text = label) },
        supportingText = supportingText?.let { text ->
            { Text(text = text) }
        },
        trailingIcon = trailingIcon,
    )
}


@Composable
internal fun InputBox(
    state: MutableState<String>,
    label: String,
    validate: (String) -> Boolean = { true },
) {
    BasicInputBox(state = state, label = label, validate = validate) {
        state.value.isNotBlank().let {
            if (it) FieldIconButton(
                Icons.TwoTone.Delete,
                stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

    val onClick = {
        focusManager.clearFocus()
        keyboardController?.hide()
        clickable()
    }

    BasicInputBox(state, label, supportingText, validate) {
        Row {
            if (showOperateIcon) {
                FieldIconButton(
                    operateIcon,
                    operateContentDescription,
                    tint = operateIconTint,
                    clickable = onClick,
                )
            }
            if (state.value.isNotBlank()) {
                FieldIconButton(
                    Icons.TwoTone.Delete,
                    stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    validate: (String) -> Boolean = { true },
    randomGenerator: () -> String,
) {
    OperateInputBox(
        state = state,
        label = label,
        validate = validate,
        operateIcon = Icons.TwoTone.Casino,
        operateContentDescription = stringResource(R.string.one_click_random),
        clickable = { state.value = randomGenerator() }
    )
}
