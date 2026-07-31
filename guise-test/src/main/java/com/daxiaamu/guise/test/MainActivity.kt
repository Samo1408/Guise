@file:Suppress("DEPRECATION", "MissingPermission")

package com.daxiaamu.guise.test

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class TestKey(@StringRes val label: Int) {
    PACKAGE_NAME(R.string.package_name),
    COMPILED_VERSION(R.string.compiled_version),
    PACKAGE_VERSION(R.string.package_manager_version),
    BRAND(R.string.brand), MANUFACTURER(R.string.manufacturer), MODEL(R.string.model),
    PRODUCT(R.string.product), DEVICE(R.string.device), BOARD(R.string.board),
    HARDWARE(R.string.hardware), FINGERPRINT(R.string.fingerprint), SDK(R.string.sdk),
    RELEASE(R.string.release), BASE_OS(R.string.base_os), DISPLAY_DENSITY(R.string.display_density),
    ANDROID_ID_SECURE(R.string.android_id_secure),
    ANDROID_ID_SYSTEM(R.string.android_id_system), IMEI(R.string.imei),
    IMEI_SLOT_1(R.string.imei_slot_1), PHONE_NUMBER(R.string.phone_number),
    NETWORK_LEGACY(R.string.network_legacy), NETWORK_MODERN(R.string.network_modern),
    WIFI_STATE(R.string.wifi_state), WIFI_SSID(R.string.wifi_ssid), WIFI_BSSID(R.string.wifi_bssid),
    WIFI_MAC(R.string.wifi_mac), SIM_OPERATOR(R.string.sim_operator), NETWORK_OPERATOR(R.string.network_operator),
    SIM_NAME(R.string.sim_name), NETWORK_NAME(R.string.network_name), SIM_COUNTRY(R.string.sim_country),
    NETWORK_COUNTRY(R.string.network_country), NETWORK_TYPE(R.string.mobile_network_type),
    SUBSCRIPTIONS(R.string.subscriptions), CELL_INFO(R.string.cell_info), LAST_LOCATION(R.string.last_location),
    PROVIDERS(R.string.location_providers), BATTERY_PROPERTY(R.string.battery_property),
    BATTERY_INTENT(R.string.battery_intent), LOCALE_LANGUAGE(R.string.locale_language),
    LOCALE_COUNTRY(R.string.locale_country), LOCALE_TAG(R.string.locale_tag), LOCALE_TEXT(R.string.locale_text),
    TIME_ZONE_JAVA(R.string.time_zone_java), TIME_ZONE_JAVA_TIME(R.string.time_zone_java_time),
    TIME_ZONE_ICU(R.string.time_zone_icu),
    CONTACTS(R.string.contacts_query), IMAGES(R.string.images_query), VIDEOS(R.string.videos_query),
    AUDIO(R.string.audio_query)
}

private data class TestResult(val value: String, val detected: Boolean = false)
private data class TestSection(@StringRes val title: Int, val keys: List<TestKey>)

private val sections = listOf(
    TestSection(R.string.section_version, listOf(TestKey.PACKAGE_NAME, TestKey.COMPILED_VERSION, TestKey.PACKAGE_VERSION)),
    TestSection(R.string.section_device, listOf(TestKey.BRAND, TestKey.MANUFACTURER, TestKey.MODEL, TestKey.PRODUCT, TestKey.DEVICE, TestKey.BOARD, TestKey.HARDWARE, TestKey.FINGERPRINT, TestKey.SDK, TestKey.RELEASE, TestKey.BASE_OS, TestKey.DISPLAY_DENSITY)),
    TestSection(
        R.string.section_identifiers,
        listOf(
            TestKey.ANDROID_ID_SECURE,
            TestKey.ANDROID_ID_SYSTEM,
            TestKey.IMEI,
            TestKey.IMEI_SLOT_1,
            TestKey.PHONE_NUMBER,
        ),
    ),
    TestSection(R.string.section_network, listOf(TestKey.NETWORK_LEGACY, TestKey.NETWORK_MODERN, TestKey.WIFI_STATE, TestKey.WIFI_SSID, TestKey.WIFI_BSSID, TestKey.WIFI_MAC)),
    TestSection(R.string.section_sim, listOf(TestKey.SIM_OPERATOR, TestKey.NETWORK_OPERATOR, TestKey.SIM_NAME, TestKey.NETWORK_NAME, TestKey.SIM_COUNTRY, TestKey.NETWORK_COUNTRY, TestKey.NETWORK_TYPE, TestKey.SUBSCRIPTIONS, TestKey.CELL_INFO)),
    TestSection(R.string.section_location, listOf(TestKey.PROVIDERS, TestKey.LAST_LOCATION)),
    TestSection(R.string.section_battery, listOf(TestKey.BATTERY_PROPERTY, TestKey.BATTERY_INTENT)),
    TestSection(
        R.string.section_locale,
        listOf(
            TestKey.LOCALE_LANGUAGE,
            TestKey.LOCALE_COUNTRY,
            TestKey.LOCALE_TAG,
            TestKey.LOCALE_TEXT,
            TestKey.TIME_ZONE_JAVA,
            TestKey.TIME_ZONE_JAVA_TIME,
            TestKey.TIME_ZONE_ICU,
        ),
    ),
    TestSection(R.string.section_blank_pass, listOf(TestKey.CONTACTS, TestKey.IMAGES, TestKey.VIDEOS, TestKey.AUDIO))
)

class MainActivity : ComponentActivity() {
    private var locationUpdate by mutableStateOf<String?>(null)
    private var secureFlagTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GuiseTestApp() }
    }

    private fun requestLocation() {
        val manager = getSystemService(LocationManager::class.java)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationUpdate = getString(R.string.permission_required)
            return
        }
        locationUpdate = getString(R.string.waiting)
        runCatching {
            manager.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationUpdate = location.describe()
                }
            }, mainLooper)
        }.onFailure { locationUpdate = it.userMessage() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GuiseTestApp() {
        val context = LocalContext.current
        val dark = androidx.compose.foundation.isSystemInDarkTheme()
        val colors = if (Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (dark) darkColorScheme() else lightColorScheme()
        var refresh by remember { mutableIntStateOf(0) }
        var results by remember { mutableStateOf<Map<TestKey, TestResult>?>(null) }
        val permissions = remember { dangerousPermissions() }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refresh++
        }

        LaunchedEffect(refresh) {
            results = null
            results = withContext(Dispatchers.IO) { collectResults(context) }
        }

        MaterialTheme(colorScheme = colors) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                topBar = {
                    CenterAlignedTopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            TextButton(onClick = { refresh++ }) {
                                Text(stringResource(R.string.refresh))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { IntroCard() }
                    if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(R.string.permissions_explanation))
                                    Button(onClick = { permissionLauncher.launch(permissions) }) {
                                        Text(stringResource(R.string.grant_permissions))
                                    }
                                }
                            }
                        }
                    }
                    if (results == null) {
                        item { Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
                    } else {
                        items(sections) { section -> ResultCard(section, results.orEmpty()) }
                    }
                    item {
                        ScreenshotCard(
                            secure = run { secureFlagTick; window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0 },
                            onAdd = { window.addFlags(WindowManager.LayoutParams.FLAG_SECURE); secureFlagTick++ },
                            onClear = { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE); secureFlagTick++ }
                        )
                    }
                    item {
                        LocationUpdateCard(locationUpdate = locationUpdate, onRequest = ::requestLocation)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.test_instructions_title), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.test_instructions))
            Text(stringResource(R.string.hook_toast_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResultCard(section: TestSection, results: Map<TestKey, TestResult>) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(section.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            section.keys.forEach { key ->
                val result = results[key] ?: TestResult(stringResource(R.string.not_available))
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(stringResource(key.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(result.value, fontFamily = FontFamily.Monospace, color = if (result.detected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    if (result.detected) Text(stringResource(R.string.hook_detected), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ScreenshotCard(secure: Boolean, onAdd: () -> Unit, onClear: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.section_screenshot), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (secure) stringResource(R.string.secure_enabled) else stringResource(R.string.secure_disabled), fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.screenshot_explanation), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.add_secure)) }
                FilledTonalButton(onClick = onClear) { Text(stringResource(R.string.clear_secure)) }
            }
        }
    }
}

@Composable
private fun LocationUpdateCard(locationUpdate: String?, onRequest: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.live_location), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(locationUpdate ?: stringResource(R.string.not_requested), fontFamily = FontFamily.Monospace)
            Button(onClick = onRequest) { Text(stringResource(R.string.request_location)) }
        }
    }
}

private fun dangerousPermissions(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.READ_PHONE_NUMBERS)
    add(Manifest.permission.READ_CONTACTS)
    if (Build.VERSION.SDK_INT >= 33) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
        add(Manifest.permission.READ_MEDIA_IMAGES)
        add(Manifest.permission.READ_MEDIA_VIDEO)
        add(Manifest.permission.READ_MEDIA_AUDIO)
    }
    if (Build.VERSION.SDK_INT >= 34) add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
}.toTypedArray()

private fun collectResults(context: Context): Map<TestKey, TestResult> = buildMap {
    fun putSafe(key: TestKey, block: () -> Any?) {
        put(key, runCatching { TestResult(block()?.toString() ?: context.getString(R.string.null_value)) }
            .getOrElse { TestResult(it.userMessage()) })
    }
    putSafe(TestKey.PACKAGE_NAME) { context.packageName }
    putSafe(TestKey.COMPILED_VERSION) { "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})" }
    this[TestKey.PACKAGE_VERSION] = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val text = "${info.versionName} (${info.longVersionCode})"
        TestResult(text, info.versionName != BuildConfig.VERSION_NAME || info.longVersionCode != BuildConfig.VERSION_CODE.toLong())
    }.getOrElse { TestResult(it.userMessage()) }
    putSafe(TestKey.BRAND) { Build.BRAND }; putSafe(TestKey.MANUFACTURER) { Build.MANUFACTURER }
    putSafe(TestKey.MODEL) { Build.MODEL }; putSafe(TestKey.PRODUCT) { Build.PRODUCT }
    putSafe(TestKey.DEVICE) { Build.DEVICE }; putSafe(TestKey.BOARD) { Build.BOARD }
    putSafe(TestKey.HARDWARE) { Build.HARDWARE }; putSafe(TestKey.FINGERPRINT) { Build.FINGERPRINT }
    putSafe(TestKey.SDK) { Build.VERSION.SDK_INT }; putSafe(TestKey.RELEASE) { Build.VERSION.RELEASE }
    putSafe(TestKey.BASE_OS) { Build.VERSION.BASE_OS }
    putSafe(TestKey.DISPLAY_DENSITY) {
        val metrics = context.resources.displayMetrics
        val configuration = context.resources.configuration
        "metrics=${metrics.densityDpi} dpi, configuration=${configuration.densityDpi} dpi, " +
            "density=${metrics.density}, scaledDensity=${metrics.scaledDensity}, " +
            "size=${configuration.screenWidthDp}×${configuration.screenHeightDp} dp, " +
            "smallest=${configuration.smallestScreenWidthDp} dp"
    }
    putSafe(TestKey.ANDROID_ID_SECURE) { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }
    putSafe(TestKey.ANDROID_ID_SYSTEM) { Settings.System.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }

    val telephony = context.getSystemService(TelephonyManager::class.java)
    putSafe(TestKey.IMEI) { telephony.getImei(0) }
    putSafe(TestKey.IMEI_SLOT_1) { telephony.getImei(1) }
    putSafe(TestKey.PHONE_NUMBER) { telephony.line1Number }

    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    putSafe(TestKey.NETWORK_LEGACY) { connectivity.activeNetworkInfo?.let { "${it.typeName} (${it.type}), subtype=${it.subtypeName}" } }
    putSafe(TestKey.NETWORK_MODERN) {
        val network = connectivity.activeNetwork ?: return@putSafe null
        val caps = connectivity.getNetworkCapabilities(network) ?: return@putSafe null
        buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WIFI")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("CELLULAR")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) add("VALIDATED")
        }.joinToString()
    }
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    putSafe(TestKey.WIFI_STATE) { "enabled=${wifi.isWifiEnabled}, state=${wifi.wifiState}, scan=${wifi.isScanAlwaysAvailable}" }
    putSafe(TestKey.WIFI_SSID) { wifi.connectionInfo.ssid }
    putSafe(TestKey.WIFI_BSSID) { wifi.connectionInfo.bssid }
    putSafe(TestKey.WIFI_MAC) { wifi.connectionInfo.macAddress }

    putSafe(TestKey.SIM_OPERATOR) { telephony.simOperator }
    putSafe(TestKey.NETWORK_OPERATOR) { telephony.networkOperator }
    putSafe(TestKey.SIM_NAME) { telephony.simOperatorName }
    putSafe(TestKey.NETWORK_NAME) { telephony.networkOperatorName }
    putSafe(TestKey.SIM_COUNTRY) { telephony.simCountryIso }
    putSafe(TestKey.NETWORK_COUNTRY) { telephony.networkCountryIso }
    putSafe(TestKey.NETWORK_TYPE) { "${telephony.networkType} / data=${telephony.dataNetworkType}" }
    putSafe(TestKey.SUBSCRIPTIONS) {
        context.getSystemService(SubscriptionManager::class.java).activeSubscriptionInfoList
            ?.joinToString("\n") { "id=${it.subscriptionId}, mcc=${it.mccString}, mnc=${it.mncString}, carrier=${it.carrierName}, country=${it.countryIso}" }
    }
    putSafe(TestKey.CELL_INFO) { telephony.allCellInfo?.joinToString("\n", transform = CellInfo::describe) }

    val locations = context.getSystemService(LocationManager::class.java)
    putSafe(TestKey.PROVIDERS) { locations.allProviders.joinToString() }
    putSafe(TestKey.LAST_LOCATION) { locations.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.describe() }

    val battery = context.getSystemService(BatteryManager::class.java)
    putSafe(TestKey.BATTERY_PROPERTY) { "${battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%" }
    putSafe(TestKey.BATTERY_INTENT) {
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        "${intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)} / ${intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)}"
    }

    val locale = Locale.getDefault()
    putSafe(TestKey.LOCALE_LANGUAGE) { locale.language }
    putSafe(TestKey.LOCALE_COUNTRY) { locale.country }
    putSafe(TestKey.LOCALE_TAG) { locale.toLanguageTag() }
    putSafe(TestKey.LOCALE_TEXT) { locale.toString() }
    putSafe(TestKey.TIME_ZONE_JAVA) { java.util.TimeZone.getDefault().id }
    putSafe(TestKey.TIME_ZONE_JAVA_TIME) { java.time.ZoneId.systemDefault().id }
    putSafe(TestKey.TIME_ZONE_ICU) { android.icu.util.TimeZone.getDefault().id }

    putQuery(context, TestKey.CONTACTS, ContactsContract.Contacts.CONTENT_URI)
    putQuery(context, TestKey.IMAGES, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    putQuery(context, TestKey.VIDEOS, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    putQuery(context, TestKey.AUDIO, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
}

private fun MutableMap<TestKey, TestResult>.putQuery(context: Context, key: TestKey, uri: android.net.Uri) {
    this[key] = runCatching {
        val cursor = context.contentResolver.query(uri, arrayOf("_id"), null, null, null)
            ?: return@runCatching TestResult(context.getString(R.string.null_cursor), detected = true)
        cursor.use { TestResult(context.getString(R.string.cursor_count, it.count)) }
    }.getOrElse { TestResult(it.userMessage()) }
}

private fun Location.describe(): String = "provider=$provider, lat=$latitude, lng=$longitude, accuracy=$accuracy, time=$time"

private fun CellInfo.describe(): String {
    val identity = when (this) {
        is CellInfoGsm -> cellIdentity
        is CellInfoLte -> cellIdentity
        is CellInfoWcdma -> cellIdentity
        is CellInfoCdma -> cellIdentity
        is CellInfoTdscdma -> cellIdentity
        is CellInfoNr -> cellIdentity
        else -> return javaClass.simpleName
    }
    return when (identity) {
        is CellIdentityGsm -> "GSM mcc=${identity.mccString}, mnc=${identity.mncString}, lac=${identity.lac}, cid=${identity.cid}"
        is CellIdentityLte -> "LTE mcc=${identity.mccString}, mnc=${identity.mncString}, tac=${identity.tac}, ci=${identity.ci}"
        is CellIdentityWcdma -> "WCDMA mcc=${identity.mccString}, mnc=${identity.mncString}, lac=${identity.lac}, cid=${identity.cid}"
        is CellIdentityCdma -> "CDMA networkId=${identity.networkId}, baseStationId=${identity.basestationId}"
        is CellIdentityTdscdma -> "TDSCDMA mcc=${identity.mccString}, mnc=${identity.mncString}, lac=${identity.lac}, cid=${identity.cid}"
        is CellIdentityNr -> "NR mcc=${identity.mccString}, mnc=${identity.mncString}, tac=${identity.tac}, nci=${identity.nci}"
        else -> identity.javaClass.simpleName
    }
}

private fun Throwable.userMessage(): String = "${javaClass.simpleName}: ${message.orEmpty()}"
