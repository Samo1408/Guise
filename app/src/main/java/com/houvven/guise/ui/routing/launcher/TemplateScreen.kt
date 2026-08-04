package com.houvven.guise.ui.routing.launcher

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoNotDisturbOnTotalSilence
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.components.simplify.SimplifyDropdownMenuItem
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.components.simplify.SimplifyImage
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.ui.routing.NavRoutingTypes
import com.houvven.guise.ui.routing.navigateWithTemplate
import com.houvven.guise.ui.routing.template.EnableTemplateDialog
import com.houvven.guise.ui.utils.saveFileToDownloadDir
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private object TemplateTypeFilter {
    const val ALL = -1
    const val COMMON = Template.Type.COMMON
    const val EXCLUSIVE = Template.Type.EXCLUSIVE
}

private val typeFilter = mutableIntStateOf(TemplateTypeFilter.ALL)
private val requestEnable = mutableStateOf(false)
private val requestEnableTemplate = mutableStateOf<Template?>(null)


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateCard(template: Template) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val installed = remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }


    val headIcon = @Composable {
        if (template.type == TemplateTypeFilter.EXCLUSIVE) {
            val modifier = Modifier
                .size(25.dp)
                .padding(bottom = 5.dp)
            runCatching {
                context.packageManager.getApplicationIcon(template.packageName!!)
            }.onFailure {
                installed.value = false
                SimplifyImage(
                    Icons.Default.DoNotDisturbOnTotalSilence,
                    modifier = modifier
                )
            }.onSuccess {
                val bitmap = it.toBitmap().asImageBitmap()
                SimplifyImage(bitmap, modifier)
            }
        }
    }

    val content = @Composable {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            headIcon()
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (template.type == Template.Type.EXCLUSIVE) {
                Text(
                    text = template.packageName!!,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (template.description.isNullOrBlank().not()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = template.description!!, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (template.type == Template.Type.EXCLUSIVE && !installed.value) {
                        GlobalSnackbarHost.showOnErrorByDismissPrevious(
                            resources.getString(R.string.exclusive_template_app_not_installed)
                        )
                    } else if (template.type == Template.Type.EXCLUSIVE) {
                        requestEnable.value = true
                        requestEnableTemplate.value = template
                    } else {
                        val navHostController = LocalNavController.current
                        navHostController.navigateWithTemplate(
                            NavRoutingTypes.ENABLE_TEMPLATE.name,
                            template,
                        )
                    }
                },
                onLongClick = {
                    expanded = true
                }
            ),
        /* colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inversePrimary.copy(.4F)
        ), */
        shape = RoundedCornerShape(10.dp)
    ) {
        content()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(10.dp, (-5).dp)
        ) {
            SimplifyDropdownMenuItem(text = stringResource(R.string.edit), onClick = {
                expanded = false
                LocalNavController.current.navigateWithTemplate(
                    NavRoutingTypes.EDIT_TEMPLATE.name,
                    template,
                )
            })
            SimplifyDropdownMenuItem(text = stringResource(R.string.delete), onClick = {
                expanded = false
                LauncherState.deleteTemplate(template)
            })
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun TemplateScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navController = LocalNavController.current

    val topBar = @Composable {
        var topBarMenuExpanded by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text(stringResource(R.string.action_template)) },
            actions = {
                IconButton(onClick = { topBarMenuExpanded = true }) {
                    SimplifyIcon(Icons.Default.MoreVert)
                }
                DropdownMenu(
                    expanded = topBarMenuExpanded,
                    onDismissRequest = { topBarMenuExpanded = false })
                {

                    val resultLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { result ->
                        topBarMenuExpanded = false
                        if (result == null) return@rememberLauncherForActivityResult
                        runCatching {
                            context.contentResolver.openInputStream(result)?.bufferedReader()?.use {
                                Json.decodeFromString<List<Template>>(it.readText())
                            } ?: error("Unable to open selected file")
                        }.onSuccess { templates ->
                            LauncherState.addTemplates(templates)
                            GlobalSnackbarHost.showByDismissPrevious(
                                resources.getString(R.string.import_success)
                            )
                        }.onFailure {
                            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                resources.getString(R.string.import_failed, it.message.orEmpty())
                            )
                        }
                    }

                    SimplifyDropdownMenuItem(
                        text = stringResource(R.string.import_data),
                        onClick = {
                            resultLauncher.launch("application/json")
                        }
                    )
                    SimplifyDropdownMenuItem(
                        text = stringResource(R.string.export_data),
                        onClick = {
                            saveFileToDownloadDir(
                                "Guise-Template-${System.currentTimeMillis()}.json",
                                Json.encodeToString(LauncherState.templates.value)
                            ).onSuccess {
                                GlobalSnackbarHost.showByDismissPrevious(
                                    resources.getString(R.string.export_success, it)
                                )
                            }.onFailure {
                                GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                    resources.getString(R.string.export_failed, it.message.orEmpty())
                                )
                            }
                        }
                    )
                }
            }
        )
    }

    val floatingButton = @Composable {
        FloatingActionButton(
            onClick = { navController.navigate(NavRoutingTypes.ADD_TEMPLATE.name) },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            SimplifyIcon(Icons.Default.Add)
        }
    }

    @Composable
    fun TypeFilter() {
        val templates = LauncherState.templates.value
        val commonCount = templates.count { it.type == Template.Type.COMMON }
        val exclusiveCount = templates.count { it.type == Template.Type.EXCLUSIVE }

        @Composable
        fun TypeFilterChip(label: String, count: Int, value: Int) {
            FilterChip(
                selected = typeFilter.intValue == value,
                onClick = { typeFilter.intValue = value },
                label = {
                    Text(stringResource(R.string.template_type_with_count, label, count))
                }
            )
        }
        Row(modifier = Modifier.padding(start = 15.dp)) {
            TypeFilterChip(
                label = stringResource(R.string.template_type_all),
                count = templates.size,
                value = TemplateTypeFilter.ALL
            )
            Spacer(modifier = Modifier.width(5.dp))
            TypeFilterChip(
                label = stringResource(R.string.template_type_common),
                count = commonCount,
                value = TemplateTypeFilter.COMMON
            )
            Spacer(modifier = Modifier.width(5.dp))
            TypeFilterChip(
                label = stringResource(R.string.template_type_app),
                count = exclusiveCount,
                value = TemplateTypeFilter.EXCLUSIVE
            )
        }
    }


    // 脚手架
    Scaffold(
        topBar = topBar,
        floatingActionButton = floatingButton,
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding()
            )
        ) {
            TypeFilter()
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                val items = if (typeFilter.intValue != TemplateTypeFilter.ALL)
                    LauncherState.templates.value.filter { it.type == typeFilter.intValue }
                else
                    LauncherState.templates.value

                items(items) { template -> TemplateCard(template) }

            }

            requestEnableTemplate.value?.let {
                EnableTemplateDialog(state = requestEnable, template = it)
            }
        }
    }
}
