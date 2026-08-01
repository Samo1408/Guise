package com.houvven.guise.ui.routing.template

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.module.apps.AppInfo
import com.houvven.guise.ui.components.AppIcon
import com.houvven.guise.ui.components.simplify.SimplifyIcon
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.guise.xposed.config.ModuleConfigManager
import java.text.Collator
import java.util.Locale

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
    val apps by remember { mutableStateOf(LauncherState.apps.value) }

    val selects = remember {
        mutableStateListOf(
            *apps
                .filter {
                    val config = ModuleConfig.get(it.packageName)
                    config.enabled && config.hasSameParameters(templateConfig)
                }
                .map { it.packageName }.toTypedArray()
        )
    }

    val unselects = remember { mutableStateListOf<String>() }

    fun filterApps() =
        apps.toList()
            .filter { if (selectedTabIndex == 0) !it.isSystemApp else it.isSystemApp }
            .sortedBy { Collator.getInstance(Locale.CHINA).getCollationKey(it.label) }
            .sortedWith { o1, o2 ->
                if (selects.contains(o1.packageName) == selects.contains(o2.packageName)) 0
                else if (selects.contains(o1.packageName)) -1
                else 1
            }

    @Composable
    fun ItemCard(appInfo: AppInfo) {
        val selected = selects.contains(appInfo.packageName)
        val colors =
            if (selected) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.inversePrimary)
            else CardDefaults.outlinedCardColors()
        val onclick =
            fun() {
                if (selected) {
                    selects.remove(appInfo.packageName)
                    unselects.add(appInfo.packageName)
                } else {
                    selects.add(appInfo.packageName)
                    unselects.remove(appInfo.packageName)
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

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(filterApps()) { appInfo -> ItemCard(appInfo) }
            }
        }
    }


    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                unselects.forEach {
                    ModuleConfigManager.of(ModuleConfig.get(it))
                        .setEnabled(false, notifyOnScopeError = false)
                }
                selects.forEach {
                    ModuleConfigManager.of(
                        templateConfig.copy(packageName = it, enabled = true)
                    ).setEnabled(true, notifyOnScopeError = false)
                }
            }
        }
    }

    DisposableEffect(lifecycle, lifecycleObserver) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycle.removeObserver(lifecycleObserver) }
    }

}
