package com.houvven.guise.ui.routing.template

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.module.apps.AppInfo
import com.houvven.guise.module.apps.AppInfoProvider
import com.houvven.guise.ui.components.AppIcon
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.guise.xposed.config.ModuleConfigManager
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class TemplateSelectionSnapshot(
    val apps: List<AppInfo>,
    val selectedPackages: Set<String>,
    val configsByPackage: Map<String, ModuleConfig>,
    val templateNamesBySignature: Map<String, String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableTemplateScreen(template: Template) {

    val templateConfig = remember(template.configuration) {
        ModuleConfig.fromJson(template.configuration)
    }

    val availableTemplates = LauncherState.templates.value
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var refreshing by remember { mutableStateOf(true) }
    var selectionLoaded by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var displayMenu by rememberSaveable { mutableStateOf(false) }
    var displaySystemApps by rememberSaveable { mutableStateOf(false) }
    var searchByPackageName by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var initiallySelected by remember(template.configuration) {
        mutableStateOf<Set<String>>(emptySet())
    }
    val selects = remember(template.configuration) { mutableStateMapOf<String, Unit>() }
    val approvedReplacements = remember(template.configuration) {
        mutableStateMapOf<String, Unit>()
    }
    var configsByPackage by remember(template.configuration) {
        mutableStateOf<Map<String, ModuleConfig>>(emptyMap())
    }
    var templateNamesBySignature by remember(template.configuration) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var pendingReplacement by remember(template.configuration) {
        mutableStateOf<AppInfo?>(null)
    }
    var prioritizedPackages by remember(template.configuration) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var changesApplied by remember(template.configuration) { mutableStateOf(false) }

    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val collator = Collator.getInstance(Locale.CHINA)
        AppInfoProvider.getList().sortedBy { collator.getCollationKey(it.label) }
    }

    fun refreshInstalledApps() {
        if (refreshing) return
        coroutineScope.launch {
            refreshing = true
            try {
                apps = loadInstalledApps()
                LauncherState.apps.value = apps
                prioritizedPackages = selects.keys.toSet()
            } finally {
                refreshing = false
            }
        }
    }

    LaunchedEffect(template.configuration) {
        val snapshot = withContext(Dispatchers.IO) {
            val sortedApps = loadInstalledApps()
            val templateSignature = templateConfig.parameterSignature()
            val configs = ModuleConfig.getAllSaved()
            val configuredPackages = configs
                .asSequence()
                .filter { it.enabled && it.parameterSignature() == templateSignature }
                .mapTo(mutableSetOf()) { it.packageName }
            val selectedPackages = sortedApps
                .asSequence()
                .map { it.packageName }
                .filterTo(mutableSetOf()) { it in configuredPackages }
            TemplateSelectionSnapshot(
                apps = sortedApps,
                selectedPackages = selectedPackages,
                configsByPackage = configs.associateBy { it.packageName },
                templateNamesBySignature = availableTemplates.associate {
                    ModuleConfig.fromJson(it.configuration).parameterSignature() to it.name
                },
            )
        }
        apps = snapshot.apps
        LauncherState.apps.value = snapshot.apps
        initiallySelected = snapshot.selectedPackages
        configsByPackage = snapshot.configsByPackage
        templateNamesBySignature = snapshot.templateNamesBySignature
        selects.clear()
        snapshot.selectedPackages.forEach { selects[it] = Unit }
        prioritizedPackages = snapshot.selectedPackages
        selectionLoaded = true
        refreshing = false
    }

    fun applySelectionChanges() {
        if (changesApplied || !selectionLoaded) return
        changesApplied = true

        ModuleConfigManager.applyTemplateSelection(
            templateConfig = templateConfig,
            initiallySelected = initiallySelected,
            selectedNow = selects.keys.toSet(),
            notifyOnScopeError = false,
        )
    }

    val filteredApps by remember {
        derivedStateOf {
            var result = if (displaySystemApps) apps else apps.filterNot(AppInfo::isSystemApp)
            if (searchQuery.isNotBlank()) {
                result = result.filter { appInfo ->
                    appInfo.label.contains(searchQuery, ignoreCase = true) ||
                        searchByPackageName && appInfo.packageName.contains(
                            searchQuery,
                            ignoreCase = true,
                        )
                }
            }
            val (prioritized, remaining) = result.partition {
                it.packageName in prioritizedPackages
            }
            prioritized + remaining
        }
    }

    fun toggleSelection(appInfo: AppInfo) {
        val selected = selects.containsKey(appInfo.packageName)
        if (selected) {
            selects.remove(appInfo.packageName)
        } else {
            val existingConfig = configsByPackage[appInfo.packageName]
            val replacesExistingConfiguration = existingConfig?.enabled == true &&
                !existingConfig.hasSameParameters(templateConfig)
            if (
                replacesExistingConfiguration &&
                !approvedReplacements.containsKey(appInfo.packageName)
            ) {
                pendingReplacement = appInfo
            } else {
                selects[appInfo.packageName] = Unit
            }
        }
    }

    @Composable
    fun AppCard(appInfo: AppInfo) {
        val selected = selects.containsKey(appInfo.packageName)
        val containerColor = if (selected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.Transparent
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { toggleSelection(appInfo) },
            colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    packageName = appInfo.packageName,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = appInfo.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val details = if (appInfo.isSystemApp) {
                        "${appInfo.packageName} · ${stringResource(R.string.app_type_system)}"
                    } else {
                        appInfo.packageName
                    }
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Checkbox(
                    checked = selected,
                    onCheckedChange = { toggleSelection(appInfo) },
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        val focusRequester = remember { FocusRequester() }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            leadingIcon = { SimplifyIcon(Icons.Default.Search) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )
                        LaunchedEffect(searching) { focusRequester.requestFocus() }
                    } else {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        applySelectionChanges()
                        // 模拟系统返回键
                        LocalNavController.current.popBackStack()
                    }) {
                        SimplifyIcon(Icons.AutoMirrored.Filled.ArrowBack)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searching) {
                            searching = false
                            searchQuery = ""
                        } else {
                            searching = true
                        }
                    }) {
                        SimplifyIcon(if (searching) Icons.Default.Close else Icons.Default.Search)
                    }
                    Box {
                        IconButton(onClick = { displayMenu = true }) {
                            SimplifyIcon(Icons.Default.Menu)
                        }
                        DropdownMenu(
                            expanded = displayMenu,
                            onDismissRequest = { displayMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.display_system_apps)) },
                                onClick = { displaySystemApps = !displaySystemApps },
                                leadingIcon = {
                                    Checkbox(
                                        checked = displaySystemApps,
                                        onCheckedChange = null,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.also_search_for_package)) },
                                onClick = { searchByPackageName = !searchByPackageName },
                                leadingIcon = {
                                    Checkbox(
                                        checked = searchByPackageName,
                                        onCheckedChange = null,
                                    )
                                },
                            )
                        }
                    }
                },
            )
        }
    ) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = ::refreshInstalledApps,
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredApps, key = AppInfo::packageName) { appInfo ->
                    AppCard(appInfo)
                }
            }
        }

        pendingReplacement?.let { appInfo ->
            val currentConfiguration = configsByPackage[appInfo.packageName]
            val currentSource = currentConfiguration
                ?.parameterSignature()
                ?.let(templateNamesBySignature::get)
                ?: stringResource(R.string.template_custom_configuration)
            AlertDialog(
                onDismissRequest = { pendingReplacement = null },
                title = { Text(stringResource(R.string.template_replace_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.template_replace_message,
                            appInfo.label,
                            currentSource,
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        approvedReplacements[appInfo.packageName] = Unit
                        selects[appInfo.packageName] = Unit
                        pendingReplacement = null
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingReplacement = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }

    BackHandler(enabled = searching) {
        searching = false
        searchQuery = ""
    }

    DisposableEffect(template.configuration) {
        onDispose(::applySelectionChanges)
    }

}
