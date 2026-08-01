package com.houvven.guise.ui.routing.launcher

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.houvven.guise.R
import com.houvven.guise.util.android.IntentUtils

private data class OpenSourceProject(
    val name: String,
    @StringRes val role: Int,
    val license: String,
    val projectUrl: String,
    val licenseUrl: String,
)

private val openSourceProjects = listOf(
    OpenSourceProject(
        name = "Guise Reborn",
        role = R.string.open_source_role_app,
        license = "GNU GPL v3.0 or later",
        projectUrl = "https://github.com/daxiaamu/Guise_Reborn",
        licenseUrl = "https://github.com/daxiaamu/Guise_Reborn/blob/main/LICENSE",
    ),
    OpenSourceProject(
        name = "AndroidX / Jetpack Compose / Material 3",
        role = R.string.open_source_role_dependency,
        license = "Apache License 2.0",
        projectUrl = "https://github.com/androidx/androidx",
        licenseUrl = "https://source.android.com/docs/setup/about/licenses",
    ),
    OpenSourceProject(
        name = "Kotlin / kotlinx.coroutines / kotlinx.serialization",
        role = R.string.open_source_role_dependency,
        license = "Apache License 2.0",
        projectUrl = "https://github.com/JetBrains/kotlin",
        licenseUrl = "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt",
    ),
    OpenSourceProject(
        name = "libxposed API / service",
        role = R.string.open_source_role_dependency,
        license = "Apache License 2.0",
        projectUrl = "https://github.com/libxposed",
        licenseUrl = "https://github.com/libxposed/api/blob/master/LICENSE",
    ),
    OpenSourceProject(
        name = "MaterialKolor / Material Color Utilities",
        role = R.string.open_source_role_dependency,
        license = "MIT License / Apache License 2.0",
        projectUrl = "https://github.com/jordond/MaterialKolor",
        licenseUrl = "https://github.com/jordond/MaterialKolor#license",
    ),
    OpenSourceProject(
        name = "MMKV",
        role = R.string.open_source_role_dependency,
        license = "BSD 3-Clause License",
        projectUrl = "https://github.com/Tencent/MMKV",
        licenseUrl = "https://github.com/Tencent/MMKV/blob/master/LICENSE.TXT",
    ),
    OpenSourceProject(
        name = "MobileModels / MobileModels-csv",
        role = R.string.open_source_role_data,
        license = "CC BY-NC-SA 4.0",
        projectUrl = "https://github.com/KHwang9883/MobileModels",
        licenseUrl = "https://creativecommons.org/licenses/by-nc-sa/4.0/",
    ),
    OpenSourceProject(
        name = "mcc-mnc-list",
        role = R.string.open_source_role_data,
        license = "MIT License",
        projectUrl = "https://github.com/pbakondy/mcc-mnc-list",
        licenseUrl = "https://github.com/pbakondy/mcc-mnc-list/blob/master/LICENSE",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpenSourceLicensesSheet(onDismiss: () -> Unit) {
    var selectedProject by remember { mutableStateOf<OpenSourceProject?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_open_source_licenses),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.settings_open_source_licenses_summary),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(openSourceProjects, key = { it.name }) { project ->
                ListItem(
                    headlineContent = { Text(project.name) },
                    supportingContent = {
                        Text("${stringResource(project.role)} · ${project.license}")
                    },
                    leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { selectedProject = project },
                )
            }
        }
    }

    selectedProject?.let { project ->
        AlertDialog(
            onDismissRequest = { selectedProject = null },
            icon = { Icon(Icons.Default.Description, contentDescription = null) },
            title = { Text(project.name) },
            text = {
                Column {
                    Text(stringResource(project.role))
                    Text(
                        text = stringResource(
                            R.string.open_source_license_label,
                            project.license,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProject = null }) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { IntentUtils.openBrowser(project.projectUrl) }) {
                        Text(stringResource(R.string.open_source_project_homepage))
                    }
                    TextButton(onClick = { IntentUtils.openBrowser(project.licenseUrl) }) {
                        Text(stringResource(R.string.open_source_view_license))
                    }
                }
            },
        )
    }
}
