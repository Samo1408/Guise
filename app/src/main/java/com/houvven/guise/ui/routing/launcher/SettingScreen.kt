package com.houvven.guise.ui.routing.launcher

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.houvven.guise.BuildConfig
import com.houvven.guise.R
import com.houvven.guise.constant.DonatePays
import com.houvven.guise.module.ktx.toBitmap
import com.houvven.guise.ui.components.simplify.SimplifyImage
import com.houvven.guise.ui.theme.ThemeMode
import com.houvven.guise.ui.theme.customThemeColor
import com.houvven.guise.ui.theme.dynamicColor
import com.houvven.guise.ui.theme.predictiveBack
import com.houvven.guise.ui.theme.setCustomThemeColor
import com.houvven.guise.ui.theme.setDynamicColor
import com.houvven.guise.ui.theme.setPredictiveBack
import com.houvven.guise.ui.theme.setThemeMode
import com.houvven.guise.ui.theme.themeMode
import com.houvven.guise.update.AppUpdateManager
import com.houvven.guise.util.android.IntentUtils
import kotlin.math.roundToInt

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
private fun LinkSettingItem(icon: ImageVector, title: String, summary: String, url: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable { IntentUtils.openBrowser(url) },
    )
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
    val showColorDialog = remember { mutableStateOf(false) }
    val showDonationDialog = remember { mutableStateOf(false) }
    val showOpenSourceLicenses = remember { mutableStateOf(false) }

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
        AnimatedVisibility(visible = !dynamicColor.value) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_custom_theme_color)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_custom_theme_color_summary))
                },
                leadingContent = {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(customThemeColor.value))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable { showColorDialog.value = true },
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_predictive_back)) },
            supportingContent = {
                Text(stringResource(R.string.settings_predictive_back_summary))
            },
            leadingContent = {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            },
            trailingContent = {
                Switch(
                    checked = predictiveBack.value,
                    onCheckedChange = ::setPredictiveBack,
                )
            },
            modifier = Modifier.clickable { setPredictiveBack(!predictiveBack.value) },
        )

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Title(text = stringResource(R.string.settings_about))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_version)) },
            supportingContent = {
                Text("${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
            },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            trailingContent = {
                TextButton(
                    onClick = { AppUpdateManager.check(manual = true) },
                    enabled = !AppUpdateManager.checking,
                ) {
                    if (AppUpdateManager.checking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.update_check_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (AppUpdateManager.detectedUpdate != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                        )
                    }
                }
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.maintainer_name)) },
            supportingContent = { Text(stringResource(R.string.maintainer_description)) },
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.original_author_name)) },
            supportingContent = {
                Text(stringResource(R.string.original_author_entry_description))
            },
            leadingContent = { Icon(Icons.Default.HistoryEdu, contentDescription = null) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable { showDonationDialog.value = true },
        )
        LinkSettingItem(
            icon = Icons.Default.Code,
            title = stringResource(R.string.settings_view_source_code),
            summary = stringResource(R.string.settings_view_source_code_summary),
            url = "https://github.com/daxiaamu/Guise_Reborn",
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_open_source_licenses)) },
            supportingContent = {
                Text(stringResource(R.string.settings_open_source_licenses_summary))
            },
            leadingContent = { Icon(Icons.Default.Gavel, contentDescription = null) },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
            modifier = Modifier.clickable { showOpenSourceLicenses.value = true },
        )
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
    if (showDonationDialog.value) {
        AlertDialog(
            onDismissRequest = { showDonationDialog.value = false },
            icon = { Icon(Icons.Default.Payments, contentDescription = null) },
            title = { Text(stringResource(R.string.original_author_name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.maintainer_donation_notice))
                    Text(stringResource(R.string.original_author_donation_description))
                    Text(
                        text = stringResource(R.string.donation_minors_warning),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.donation_nickname_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = {
                            showDonationDialog.value = false
                            receiptCode.value = DonatePays.ALIPAY.base64.toBitmap()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.donate_alipay))
                    }
                    FilledTonalButton(
                        onClick = {
                            showDonationDialog.value = false
                            receiptCode.value = DonatePays.WECHAT.base64.toBitmap()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.donate_wechat))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonationDialog.value = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    if (showOpenSourceLicenses.value) {
        OpenSourceLicensesSheet {
            showOpenSourceLicenses.value = false
        }
    }
    if (showColorDialog.value) {
        ThemeColorDialog(
            initialColor = Color(customThemeColor.value),
            onDismiss = { showColorDialog.value = false },
            onConfirm = {
                setCustomThemeColor(it.toArgb())
                showColorDialog.value = false
            },
        )
    }
}

@Composable
private fun ThemeColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    var red by remember(initialColor) { mutableFloatStateOf(initialColor.red) }
    var green by remember(initialColor) { mutableFloatStateOf(initialColor.green) }
    var blue by remember(initialColor) { mutableFloatStateOf(initialColor.blue) }
    val color = Color(red, green, blue)
    val presets = listOf(
        Color(0xFF216DFF),
        Color(0xFF6750A4),
        Color(0xFF008577),
        Color(0xFF3F7D20),
        Color(0xFFC2410C),
        Color(0xFFB3261E),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_choose_theme_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                )
                ThemeColorSlider(stringResource(R.string.color_red), red) { red = it }
                ThemeColorSlider(stringResource(R.string.color_green), green) { green = it }
                ThemeColorSlider(stringResource(R.string.color_blue), blue) { blue = it }
                Text("#${color.toArgb().toUInt().toString(16).takeLast(6).uppercase()}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    presets.forEach { preset ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    red = preset.red
                                    green = preset.green
                                    blue = preset.blue
                                }
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) { Text(stringResource(R.string.confirm)) }
        },
    )
}

@Composable
private fun ThemeColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(42.dp))
        Slider(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f))
        Text(
            "${(value * 255).roundToInt()}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
