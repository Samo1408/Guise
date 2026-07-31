package com.houvven.guise.ui.routing.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.db.DeviceDBHelper
import com.houvven.guise.module.PresetAdapter
import com.houvven.guise.module.preset.CarrierPresetRepository
import com.houvven.guise.module.preset.PresetRepository
import com.houvven.guise.ui.components.SearchBox
import com.houvven.guise.util.android.Randoms
import com.houvven.guise.xposed.config.ModuleConfigState


private val localSetValue = mutableStateOf({ _: String -> })
private val localPreset = mutableStateOf(emptyList<PresetAdapter>())

private val allBrands = DeviceDBHelper(ContextAmbient.current).use { it.getAllBrand() }

@Composable
private fun ConfigEditorItems(state: ModuleConfigState, launch: () -> Unit) {
    @Composable
    fun PresetInputBox(
        state: MutableState<String>,
        label: String,
        preset: List<PresetAdapter>,
        showOperateIcon: Boolean = true,
        validate: (String) -> Boolean = { true },
        setValue: (String) -> Unit = { value -> state.value = value },
    ) = OperateInputBox(state, label, showOperateIcon, validate) {
        localPreset.value = preset
        localSetValue.value = setValue
        launch()
    }


    val context = LocalContext.current
    val carrierPresets = remember(context) { CarrierPresetRepository.get(context) }
    val presetCatalog = remember(context) { PresetRepository.get(context) }


    Title(text = stringResource(R.string.title_device_parameter), topPadding = 1.dp)
    PresetInputBox(
        state = state.brand,
        label = stringResource(R.string.device_brand),
        preset = allBrands.map {
            object : PresetAdapter {
                override val label: String = it.value
                override val value: String = it.key
            }
        }
    )
    PresetInputBox(
        state = state.model,
        label = stringResource(R.string.device_model),
        preset = DeviceDBHelper(context).use { dbHelper ->
            dbHelper.getDevicesByBrand(state.brand.value)
                .filterNot { it.modelName.isNullOrBlank() || it.model.isNullOrBlank() }
                .map {
                    val name = if (it.verName == "#" || it.verName == null) it.modelName!!
                    else "${it.modelName!!} (${it.verName.removePrefix("#")})"
                    object : PresetAdapter {
                        override val label: String = "$name · ${it.model}"
                        override val value: String = "${it.model!!}:${it.codeAlias ?: ""}"
                    }
                }
        },
        showOperateIcon = allBrands.keys.any { it.equals(state.brand.value, ignoreCase = true) },
        setValue = { value ->
            state.model.value = value.substringBefore(":")
            state.device.value = value.substringAfter(":", missingDelimiterValue = "")
        }
    )
    InputBox(state.device, stringResource(R.string.device_device))
    InputBox(state.product, stringResource(R.string.device_product))
    InputBox(state.board, stringResource(R.string.device_board))
    InputBox(state.hardware, stringResource(R.string.device_cpu))
    PresetInputBox(
        state.androidVersion,
        stringResource(R.string.device_system_android_version),
        preset = presetCatalog.androidVersions.reversed(),
    ) { value ->
        state.androidVersion.value = value.substringBefore('|')
        value.substringAfter('|', missingDelimiterValue = "")
            .takeIf(String::isNotBlank)
            ?.let { state.sdkInt.value = it }
    }
    PresetInputBox(
        state = state.sdkInt,
        label = stringResource(R.string.device_system_api_level),
        preset = presetCatalog.sdkLevels.reversed(),
    )
    PresetInputBox(
        state = state.densityDpi,
        label = stringResource(R.string.device_display_density),
        preset = presetCatalog.densityDpi.reversed(),
        validate = { value -> value.length <= 4 && value.all(Char::isDigit) },
    )
    InputBox(state.fingerPrint, stringResource(R.string.device_system_finger_print))


    Title(text = stringResource(R.string.title_net_info))
    PresetInputBox(
        state = state.networkType,
        label = stringResource(R.string.net_type),
        preset = presetCatalog.networks,
    )
    InputBox(state.wifiSSID, stringResource(R.string.net_wifi_ssid))
    InputBox(state.wifiBSSID, stringResource(R.string.net_wifi_bssid))
    InputBox(state.wifiMacAddress, stringResource(R.string.net_wifi_mac))


    Title(text = stringResource(R.string.title_sim))
    PresetInputBox(
        state.simOperator,
        stringResource(R.string.net_sim_code),
        carrierPresets,
    ) { plmn ->
        carrierPresets.firstOrNull { it.plmn == plmn }?.let { carrier ->
            state.simOperatorName.value = carrier.name
            state.simOperator.value = carrier.plmn
            state.simCountry.value = carrier.countryCode
        }
    }
    InputBox(state.simOperatorName, stringResource(R.string.net_sim_name))
    InputBox(state.simCountry, stringResource(R.string.net_sim_iso))


    Title(text = stringResource(R.string.title_unique_id))
    RandomInputBox(state.imei, stringResource(R.string.id_imei)) { Randoms.randomIMEI() }
    RandomInputBox(
        state.phoneNum,
        stringResource(R.string.id_phone_num)
    ) { Randoms.randomPhoneNum() }
    RandomInputBox(state.androidId, stringResource(R.string.id_ssaid)) { Randoms.randomIMEI() }


    Title(text = stringResource(R.string.title_cell_location))
    InputBox(state.lac, stringResource(R.string.gsm_lac))
    InputBox(state.cid, stringResource(R.string.gsm_cid))


    Title(text = stringResource(R.string.title_location_info))
    InputBox(state.longitude, stringResource(R.string.location_lng))
    InputBox(state.latitude, stringResource(R.string.location_lat))
    ContainerSwitch(state.randomOffset, stringResource(R.string.location_offset))
    ContainerSwitch(state.makeWifiLocationFail, stringResource(R.string.location_wifi_fail))
    ContainerSwitch(state.makeCellLocationFail, stringResource(R.string.location_cell_fail))


    Title(text = stringResource(R.string.title_build_config))
    InputBox(state.versionCode, stringResource(R.string.build_config_version_code))
    InputBox(state.versionName, stringResource(R.string.build_config_version_name))


    Title(text = stringResource(R.string.title_other))
    InputBox(state.batteryLevel, stringResource(R.string.other_battery_level))
    PresetInputBox(
        state.language,
        stringResource(R.string.other_language),
        presetCatalog.languages,
    )
    ContainerSwitch(
        state.allowForceScreenshots,
        stringResource(R.string.other_allow_force_screenshots),
    )


    Title(text = stringResource(R.string.title_blank_pass))
    ContainerSwitch(state.passContacts, stringResource(R.string.pass_contacts))
    ContainerSwitch(state.passPhoto, stringResource(R.string.pass_photo))
    ContainerSwitch(state.passVideo, stringResource(R.string.pass_video))
    ContainerSwitch(state.passAudio, stringResource(R.string.pass_audio))


    // bottom blank 底部留白
    Spacer(modifier = Modifier.height(50.dp))
}
@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
internal fun ConfigEditorView(
    moduleConfigState: ModuleConfigState,
    topBar: @Composable () -> Unit,
) {
    var showPresets by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }

    if (!showPresets) {
        key = ""
    }

    val content = @Composable {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val presets =
                    if (key.isBlank()) localPreset.value
                    else localPreset.value.filter {
                        it.label.contains(key, ignoreCase = true) ||
                            it.value.contains(key, ignoreCase = true)
                    }

                SearchBox(value = key, onValueChange = { key = it })
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 15.dp),
                ) {
                    items(presets) {
                        ElevatedAssistChip(
                            onClick = { localSetValue.value(it.value); showPresets = false },
                            label = { Row(Modifier.padding(vertical = 15.dp)) { Text(it.label) } },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(50.dp))
            }
        }
    }

    Scaffold(topBar = topBar) {
        Surface(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .clipScrollableContainer(Orientation.Vertical)
            ) {
                ConfigEditorItems(moduleConfigState) { showPresets = true }
            }
        }
    }

    if (showPresets) {
        ModalBottomSheet(onDismissRequest = { showPresets = false }) { content() }
    }

}
