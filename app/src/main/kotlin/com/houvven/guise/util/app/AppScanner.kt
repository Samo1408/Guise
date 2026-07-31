package com.houvven.guise.util.app

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.highcapable.betterandroid.system.extension.component.getPackageInfoOrNull
import com.highcapable.betterandroid.system.extension.component.versionCodeCompat
import com.houvven.guise.util.isSystemApp
import com.houvven.guise.util.toImageBitmapOrEmpty
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext


/**
 * A scanner for installed apps.
 *
 * @property packageManager The [PackageManager] to use for scanning.
 */
class AppScanner(
    private val packageManager: PackageManager,
    private val excludedPackages: Set<String> = emptySet()
) {

    /**
     * Scans the installed apps and returns a flow of [App]s.
     *
     * @return A [Flow] of [App]s.
     */
    suspend fun scanAppsFlow(scanMode: ScanMode = ScanMode.ALL) =
        commonScanAppsFlow(packages = installedPackages, scanMode = scanMode)

    /**
     * Scans the installed apps for the user and returns a flow of [App]s.
     *
     * This method filters out apps that are not for the current user.
     * @return A [Flow] of [App]s.
     */
    suspend fun scanAppsFlowAsUser(scanMode: ScanMode = ScanMode.ALL) =
        commonScanAppsFlow(packages = installedPackagesAsUser, scanMode = scanMode)

    private suspend fun commonScanAppsFlow(
        packages: List<PackageInfo>,
        scanMode: ScanMode
    ) = flow {
        when (scanMode) {
            ScanMode.ALL -> packages
            ScanMode.SYSTEM -> packages.filter { it.isSystemApp }
            ScanMode.USER -> packages.filter { !it.isSystemApp }
        }.filter { it.packageName !in excludedPackages }.map {
            emit(createApp(it))
        }
    }

    suspend fun scanApps(scanMode: ScanMode = ScanMode.ALL) =
        commonScanApps(packages = installedPackages, scanMode = scanMode)

    suspend fun scanAppsAsUser(scanMode: ScanMode = ScanMode.ALL) =
        commonScanApps(packages = installedPackagesAsUser, scanMode = scanMode)

    private suspend fun commonScanApps(
        packages: List<PackageInfo>,
        scanMode: ScanMode = ScanMode.ALL
    ) = withContext(Dispatchers.Default) {
        when (scanMode) {
            ScanMode.ALL -> packages
            ScanMode.SYSTEM -> packages.filter { it.isSystemApp }
            ScanMode.USER -> packages.filter { !it.isSystemApp }
        }.filter { it.packageName !in excludedPackages }.map {
            async(context = Dispatchers.Default, start = CoroutineStart.LAZY) { createApp(it) }
        }.awaitAll()
    }


    /**
     * Gets the app with the specified package name.
     * @param packageName The package name of the app.
     * @return The app with the specified package name, or `null` if the app is not installed.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getAppAsUser(
        packageName: String,
        packages: List<PackageInfo> = this.installedPackagesAsUser
    ): App? = packages.firstOrNull { it.packageName == packageName }?.let { createApp(it) }


    suspend fun getAppsAsUser(packageNames: Set<String>) = withContext(Dispatchers.Default) {
        installedPackagesAsUser.filter { packageNames.contains(it.packageName) }
            .map { async { createApp(it) } }.awaitAll()
    }


    /**
     * Gets the apps with the specified package names.
     * @param packageNames The package names of the apps.
     * @return A flow of apps with the specified package names.
     */
    suspend fun getAppsFlowAsUser(packageNames: Set<String>): Flow<App> {
        return withContext(Dispatchers.Default) {
            flow {
                installedPackagesAsUser
                    .filter { packageNames.contains(it.packageName) }
                    .map {
                        async(context = Dispatchers.Default, start = CoroutineStart.LAZY) {
                            createApp(it)
                        }
                    }.awaitAll()
            }
        }
    }


    /**
     * Creates an [App] from the specified [PackageInfo].
     */
    private fun createApp(pi: PackageInfo) = pi.run {
        val applicationInfo = requireNotNull(applicationInfo) {
            "ApplicationInfo is missing for $packageName"
        }
        App(
            name = applicationInfo.loadLabel(packageManager).toString(),
            packageName = packageName,
            icon = applicationInfo.loadIcon(packageManager).toImageBitmapOrEmpty(),
            isSystemApp = isSystemApp,
            versionName = versionName ?: "",
            versionCode = versionCodeCompat,
            dataDir = applicationInfo.dataDir
        )
    }


    private val installedPackagesAsUser
        get() = packageManager.run {
            getInstalledApplications(0).mapNotNull { getPackageInfoOrNull(it.packageName) }
        }

    private val installedPackages get() = packageManager.getInstalledPackages(0)

    companion object {
        const val TAG = "AppScanner"
    }

    enum class ScanMode {
        ALL,
        SYSTEM,
        USER
    }
}
