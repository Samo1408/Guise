package com.houvven.guise.ui.routing.launcher

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.houvven.guise.BuildConfig
import com.houvven.guise.R
import com.houvven.guise.constant.DonatePays
import com.houvven.guise.module.ktx.toBitmap
import com.houvven.guise.ui.components.Hyperlink
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.components.simplify.SimplifyImage
import com.houvven.guise.ui.theme.ThemeMode
import com.houvven.guise.ui.theme.dynamicColor
import com.houvven.guise.ui.theme.setDynamicColor
import com.houvven.guise.ui.theme.setThemeMode
import com.houvven.guise.ui.theme.themeMode
import com.houvven.guise.ui.utils.hideLauncherIcon
import com.houvven.guise.ui.utils.isHideLauncherIcon

@Composable
private fun Title(text: String, topPadding: Dp = 30.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = topPadding),
    )
}

@Composable
private fun Container(
    verticalPadding: Dp = 1.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

@Composable
private fun ContainerSwitch(
    label: String,
    subLabel: String = "",
    state: MutableState<Boolean>,
    onChange: (Boolean) -> Unit = {},
) {
    Container(verticalPadding = 5.dp, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subLabel.isNotBlank()) {
                Text(
                    subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Switch(checked = state.value, onCheckedChange = { state.value = it; onChange(it) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSetting() {
    Column(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_app_style)) },
            supportingContent = { Text(stringResource(R.string.settings_app_style_summary)) },
            leadingContent = {
                Icon(
                    when (themeMode.value) {
                        ThemeMode.SYSTEM -> Icons.Default.Settings
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    },
                    contentDescription = null,
                )
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
            ).forEach { (mode, label) ->
                val selected = themeMode.value == mode
                OutlinedButton(
                    onClick = { setThemeMode(mode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                ) {
                    Text(label)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingScreen() {
    val receiptCode = remember { mutableStateOf<Bitmap?>(null) }

    @Composable
    fun content() {
        Title(text = stringResource(R.string.settings_appearance))
        ThemeModeSetting()
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
            supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
            leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = dynamicColor.value,
                    onCheckedChange = ::setDynamicColor,
                )
            },
            modifier = Modifier.clickable { setDynamicColor(!dynamicColor.value) },
        )

        Title(text = stringResource(R.string.settings_configuration))
        ContainerSwitch(
            label = stringResource(R.string.settings_hide_launcher_icon),
            state = remember { mutableStateOf(isHideLauncherIcon()) },
            onChange = { hideLauncherIcon(it) },
        )

        Title(text = stringResource(R.string.settings_about))
        Container(verticalPadding = 7.dp) {
            Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                modifier = Modifier.padding(start = 5.dp),
            )
        }
        Container(verticalPadding = 7.dp) {
            Text(stringResource(R.string.settings_maintainer), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.maintainer_name), modifier = Modifier.padding(start = 5.dp))
        }
        Container(verticalPadding = 7.dp) {
            Text(stringResource(R.string.settings_original_author), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.original_author_name), modifier = Modifier.padding(start = 5.dp))
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, top = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Hyperlink(
                label = stringResource(R.string.settings_update_address),
                url = "https://github.com/daxiaamu/Guise_Reborn/releases",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
            SimplifyIcon(Icons.Default.Link, tint = MaterialTheme.colorScheme.primary)
        }

        Title(text = stringResource(R.string.settings_feedback_address))
        Container(verticalPadding = 7.dp) {
            Hyperlink(
                label = "GitHub Issues",
                url = "https://github.com/daxiaamu/Guise_Reborn/issues",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Title(text = stringResource(R.string.settings_donation_channels))
        Container(verticalPadding = 7.dp) {
            Column {
                Text(stringResource(R.string.maintainer_name), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.maintainer_donation_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Container(verticalPadding = 7.dp) {
            Column {
                Text(stringResource(R.string.original_author_with_name), style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        stringResource(R.string.donate_alipay),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable {
                            receiptCode.value = DonatePays.ALIPAY.base64.toBitmap()
                        },
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        stringResource(R.string.donate_wechat),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable {
                            receiptCode.value = DonatePays.WECHAT.base64.toBitmap()
                        },
                    )
                }
            }
        }
        Container {
            Column {
                Text(
                    text = stringResource(R.string.donation_minors_warning),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(R.string.donation_nickname_note),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.action_setting)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .clipScrollableContainer(Orientation.Vertical),
        ) {
            content()
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    receiptCode.value?.let { bitmap ->
        ModalBottomSheet(onDismissRequest = { receiptCode.value = null }) {
            Spacer(modifier = Modifier.height(1.dp))
            SimplifyImage(bitmap.asImageBitmap(), contentScale = ContentScale.Fit)
        }
    }
}
