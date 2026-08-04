package com.houvven.guise.ui.routing.template

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun EnableTemplateScreen(template: Template) {

    val templateConfig = remember(template.configuration) {
        ModuleConfig.fromJson(template.configuration)
    }

    // 系统与用户APP过滤
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val availableTemplates = LauncherState.templates.value
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var refreshing by remember { mutableStateOf(true) }
    var selectionLoaded by remember { mutableStateOf(false) }
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
            val tabApps = apps.filter {
                if (selectedTabIndex == 0) !it.isSystemApp else it.isSystemApp
            }
            val (prioritized, remaining) = tabApps.partition {
                it.packageName in prioritizedPackages
            }
            prioritized + remaining
        }
    }

    @Composable
    fun ItemCard(appInfo: AppInfo) {
        val selected = selects.containsKey(appInfo.packageName)
        val colors =
            if (selected) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.inversePrimary)
            else CardDefaults.outlinedCardColors()
        val onclick =
            fun() {
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

        OutlinedCard(
            modifier = Modifier.padding(5.dp),
            colors = colors,
            onClick = onclick,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(7.dp)
                    .padding(start = 5.dp),
                // horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIcon(
                    packageName = appInfo.packageName,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = appInfo.label,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium
                    )
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
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // 判断滑动方向
                        if (delta > 0) {
                            if (selectedTabIndex == 1) selectedTabIndex = 0
                        } else {
                            if (selectedTabIndex == 0) selectedTabIndex = 1
                        }
                    }
                )
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    text = { Text(text = stringResource(R.string.user_apps)) },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                Tab(
                    text = { Text(text = stringResource(R.string.system_apps)) },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = ::refreshInstalledApps,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appInfo ->
                        ItemCard(appInfo)
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
    }


    DisposableEffect(template.configuration) {
        onDispose(::applySelectionChanges)
    }

}
