package com.houvven.guise.ui.routing.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.houvven.guise.R
import com.houvven.guise.constant.AppConfigKey
import com.houvven.guise.module.apps.AppInfo
import com.houvven.guise.module.apps.AppSortTypes
import com.houvven.guise.ui.components.simplify.NoBtnAlertDialog
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.components.simplify.SimplifyImage
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.guise.xposed.config.ModuleConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale


private val appSortType by derivedStateOf {
    mutableStateOf(
        AppSortTypes.valueOf(
            AppConfigKey.mmkv.decodeString(
                AppConfigKey.APP_SORT_TYPE,
                AppSortTypes.NAME.name
            ) ?: AppSortTypes.NAME.name
        )
    )
}
private val searchByPackageName by derivedStateOf {
    mutableStateOf(AppConfigKey.run { mmkv.decodeBool(APP_SEARCH_BY_PACKAGE_NAME, false) })
}

private val displaySystemApp by derivedStateOf {
    mutableStateOf(AppConfigKey.run { mmkv.decodeBool(DISPLAY_SYSTEM_APP, false) })
}

private val reverseOrder by derivedStateOf {
    mutableStateOf(AppConfigKey.run { mmkv.decodeBool(APP_REVERSE_SORT, false) })
}

private val searchKeyWorld by derivedStateOf { mutableStateOf("") }


private fun generateApps(prioritizedPackages: Collection<String>): List<AppInfo> {
    var result = LauncherState.apps.value
    if (!displaySystemApp.value) result = result.filterNot { it.isSystemApp }

    if (searchKeyWorld.value.isNotBlank()) {
        result = searchKeyWorld.let { key ->
            if (searchByPackageName.value) {
                result.filter {
                    it.packageName.contains(key.value, true) || it.label.contains(key.value, true)
                }
            } else {
                result.filter { it.label.contains(key.value, true) }
            }
        }
    }
    result = when (appSortType.value) {
        AppSortTypes.NAME -> result.sortedBy {
            Collator.getInstance(Locale.CHINA).getCollationKey(it.label)
        }

        AppSortTypes.PACKAGE_NAME -> result.sortedBy { it.packageName }
        AppSortTypes.INSTALL_TIME -> result.sortedBy { it.installTime }
        AppSortTypes.UPDATE_TIME -> result.sortedBy { it.updateTime }
    }
    if (reverseOrder.value) result = result.reversed()
    return result.sortedWith { a, b ->
        val aPrioritized = a.packageName in prioritizedPackages
        val bPrioritized = b.packageName in prioritizedPackages
        if (aPrioritized == bPrioritized) 0 else if (aPrioritized) -1 else 1
    }
}


@Composable
private fun AppCard(
    appInfo: AppInfo,
    onOpenConfig: (AppInfo) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val containerColor = if (appInfo.isEnable) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color.Transparent
    }
    val modifier = Modifier
        .padding(horizontal = 8.dp)
        .clip(MaterialTheme.shapes.medium)
        .background(containerColor)
        .fillMaxWidth()
        .clickable(onClick = { onOpenConfig(appInfo) })
        .padding(horizontal = 8.dp, vertical = 10.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier
            .padding(horizontal = 8.dp)
            .size(36.dp)
        SimplifyImage(appInfo.icon.asImageBitmap(), iconModifier)
        val typography = MaterialTheme.typography
        Column(Modifier.weight(1f)) {
            val appType = stringResource(
                if (appInfo.isSystemApp) R.string.app_type_system else R.string.app_type_user
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = appInfo.label,
                    modifier = Modifier.weight(1f),
                    style = typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = appType,
                    modifier = Modifier.padding(start = 6.dp, end = 4.dp),
                    style = typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    maxLines = 1,
                )
            }
            Text(
                text = appInfo.packageName,
                style = typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = appInfo.isEnable,
            onCheckedChange = onEnabledChange,
        )
    }
}


@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun DeployScreen(onOpenConfig: (AppInfo) -> Unit) {

    var displayMenu by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var refreshing by rememberSaveable { mutableStateOf(false) }
    var prioritizedPackages by rememberSaveable {
        mutableStateOf(
            LauncherState.apps.value.filter { it.isEnable }.map { it.packageName }
        )
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            refreshing = true
            val apps = withContext(Dispatchers.Default) {
                com.houvven.guise.module.apps.AppInfoProvider.getList()
            }
            LauncherState.apps.value = apps
            prioritizedPackages = apps.filter { it.isEnable }.map { it.packageName }
            refreshing = false
        }
    }

    val cancelSearch = { searching = false; searchKeyWorld.value = "" }


    @Composable
    fun Bar() {
        val focusRequester = remember { FocusRequester() }
        // val focusManager = LocalFocusManager.current
        val title = @Composable {
            if (searching) {
                OutlinedTextField(
                    value = searchKeyWorld.value,
                    onValueChange = { searchKeyWorld.value = it },
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { SimplifyIcon(Icons.Default.Search) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                // 将焦点移动到搜索框
                LaunchedEffect(searching) {
                    focusRequester.requestFocus()
                    // focusManager.moveFocus(FocusDirection.Down)
                }
            } else Text(
                stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.W500
            )
        }
        val actions = @Composable {
            if (searching.not()) IconButton(onClick = { searching = true }) {
                SimplifyIcon(Icons.Default.Search)
            } else IconButton(onClick = cancelSearch) {
                SimplifyIcon(Icons.Default.Close)
            }
            IconButton(onClick = { displayMenu = true }) { SimplifyIcon(Icons.Default.Menu) }
        }

        TopAppBar(title = title, actions = { actions() })
    }


    @Composable
    fun Menu() {
        val modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))

        @Composable
        fun RadioItem(text: String, value: AppSortTypes, state: MutableState<AppSortTypes>) {
            Row(
                modifier = modifier.clickable(onClick = { state.value = value }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == state.value, onClick = { state.value = value })
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        @Composable
        fun CheckboxItem(text: String, state: MutableState<Boolean>) {
            Row(
                modifier = modifier.clickable(onClick = { state.value = !state.value }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = state.value, onCheckedChange = { state.value = it })
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        NoBtnAlertDialog(onDismissRequest = {
            displayMenu = false
            AppConfigKey.mmkv.run {
                /* putBoolean(AppConfigKey.APP_REVERSE_SORT, reverseOrder.value)
                putBoolean(AppConfigKey.DISPLAY_SYSTEM_APP, displaySystemApp.value)
                putBoolean(AppConfigKey.APP_SEARCH_BY_PACKAGE_NAME, searchByPackageName.value)
                putString(AppConfigKey.APP_SORT_TYPE, appSortType.value.name) */
                encode(AppConfigKey.APP_SORT_TYPE, appSortType.value.name)
                encode(AppConfigKey.APP_SEARCH_BY_PACKAGE_NAME, searchByPackageName.value)
                encode(AppConfigKey.DISPLAY_SYSTEM_APP, displaySystemApp.value)
                encode(AppConfigKey.APP_REVERSE_SORT, reverseOrder.value)
            }

        }) {
            Column {
                AppSortTypes.values().forEach { RadioItem(it.depict, it, appSortType) }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                CheckboxItem(
                    text = stringResource(R.string.also_search_for_package),
                    state = searchByPackageName
                )
                CheckboxItem(
                    text = stringResource(R.string.display_system_apps),
                    state = displaySystemApp
                )
                CheckboxItem(
                    text = stringResource(R.string.apps_reverse_order),
                    state = reverseOrder
                )
            }
        }
    }


    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { Bar() }
    ) { pd ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(top = pd.calculateTopPadding(), bottom = pd.calculateBottomPadding())
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = generateApps(prioritizedPackages),
                    key = { appInfo -> appInfo.packageName },
                ) { appInfo ->
                    val restartToApplyMessage = stringResource(
                        R.string.restart_app_to_apply_configuration,
                        appInfo.label,
                    )
                    AppCard(
                        appInfo = appInfo,
                        onOpenConfig = onOpenConfig,
                        onEnabledChange = { enabled ->
                            val manager = ModuleConfigManager.of(
                                ModuleConfig.get(appInfo.packageName)
                            )
                            if (enabled) {
                                manager.setEnabled(true)
                                com.houvven.guise.ui.GlobalSnackbarHost.showByDismissPrevious(
                                    restartToApplyMessage
                                )
                            } else {
                                coroutineScope.launch {
                                    val stopResult = manager.stopIfHooked()
                                    manager.setEnabled(false)
                                    stopResult.fold(
                                        onSuccess = {
                                            com.houvven.guise.ui.GlobalSnackbarHost
                                                .showByDismissPrevious(restartToApplyMessage)
                                        },
                                        onFailure = {
                                            com.houvven.guise.ui.GlobalSnackbarHost
                                                .showOnErrorByDismissPrevious(
                                                    "$restartToApplyMessage\n${it.message ?: it}"
                                                )
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
        LaunchedEffect(Unit) {
            if (LauncherState.apps.value.isEmpty()) onRefresh()
        }
        if (displayMenu) Menu()
    }

    BackHandler(
        enabled = searching,
        onBack = cancelSearch
    )

    // 生命周期
    DisposableEffect(Unit) {
        onDispose {
            searchKeyWorld.value = ""
        }
    }
}
