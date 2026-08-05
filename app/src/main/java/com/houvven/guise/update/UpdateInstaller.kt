package com.houvven.guise.update

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.houvven.guise.R
import com.houvven.guise.ui.theme.GuiseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val expected = context.getSharedPreferences(
            AppUpdater.UPDATE_PREFERENCES,
            Context.MODE_PRIVATE,
        ).getLong(AppUpdater.KEY_DOWNLOAD_ID, -1L)
        val completed = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (expected < 0L || completed != expected) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val verified = UpdateInstaller.isSuccessful(context, completed) &&
                    UpdateInstaller.isVerified(
                        context,
                        completed,
                        AppUpdater().expectedSha256(context),
                        context.getSharedPreferences(
                            AppUpdater.UPDATE_PREFERENCES,
                            Context.MODE_PRIVATE,
                        ).getInt(AppUpdater.KEY_DOWNLOAD_VERSION_CODE, -1),
                    )
                if (verified) {
                    val preferences = context.getSharedPreferences(
                        AppUpdater.UPDATE_PREFERENCES,
                        Context.MODE_PRIVATE,
                    )
                    UpdateInstaller.markReady(
                        context = context,
                        downloadId = completed,
                        versionCode = preferences.getInt(AppUpdater.KEY_DOWNLOAD_VERSION_CODE, -1),
                        sha256 = AppUpdater().expectedSha256(context),
                    )
                    UpdateInstaller.showInstallNotification(context, completed)
                } else {
                    AppUpdater().retryNextDownload(context, completed)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object UpdateInstaller {
    private const val CHANNEL_ID = "app_update_install"
    private const val NOTIFICATION_ID = 0x4755
    private const val ROOT_INSTALL_TIMEOUT_SECONDS = 180L

    fun markReady(
        context: Context,
        downloadId: Long,
        versionCode: Int,
        sha256: String,
    ) {
        context.getSharedPreferences(AppUpdater.UPDATE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putLong(AppUpdater.KEY_READY_DOWNLOAD_ID, downloadId)
            .putInt(AppUpdater.KEY_READY_VERSION_CODE, versionCode)
            .putString(AppUpdater.KEY_READY_SHA256, sha256.lowercase())
            .apply()
    }

    fun pending(context: Context, versionCode: Int, sha256: String): Long {
        val preferences = context.getSharedPreferences(
            AppUpdater.UPDATE_PREFERENCES,
            Context.MODE_PRIVATE,
        )
        val matches = readyArtifactMatches(
            expectedVersionCode = versionCode,
            expectedSha256 = sha256,
            readyVersionCode = preferences.getInt(AppUpdater.KEY_READY_VERSION_CODE, -1),
            readySha256 = preferences.getString(AppUpdater.KEY_READY_SHA256, null),
        )
        return if (matches) preferences.getLong(AppUpdater.KEY_READY_DOWNLOAD_ID, -1L) else -1L
    }

    fun isSuccessful(context: Context, downloadId: Long): Boolean {
        val manager = context.getSystemService(DownloadManager::class.java)
        return runCatching {
            manager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                cursor.moveToFirst() &&
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                    DownloadManager.STATUS_SUCCESSFUL &&
                    manager.getUriForDownloadedFile(downloadId) != null
            }
        }.getOrDefault(false)
    }

    suspend fun isVerified(
        context: Context,
        downloadId: Long,
        expectedSha256: String,
        expectedVersionCode: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        if (expectedVersionCode <= 0) return@withContext false
        val manager = context.getSystemService(DownloadManager::class.java)
        val cachedApk = File(context.cacheDir, "updates/verify-$downloadId.apk")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            cachedApk.parentFile?.mkdirs()
            manager.openDownloadedFile(downloadId).use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { input ->
                    cachedApk.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
            }
            val actualSha256 = digest.digest().joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
            val hashMatches = expectedSha256.isBlank() ||
                actualSha256.equals(expectedSha256, ignoreCase = true)
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    cachedApk.absolutePath,
                    PackageManager.PackageInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(cachedApk.absolutePath, 0)
            }
            val archiveVersionCode = archiveInfo?.let { info ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            }
            hashMatches &&
                archiveInfo != null &&
                archiveInfo.packageName == context.packageName &&
                archiveVersionCode == expectedVersionCode.toLong()
        } catch (_: Exception) {
            false
        } finally {
            cachedApk.delete()
        }
    }

    fun showInstallNotification(context: Context, downloadId: Long) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.update_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.update_notification_channel_summary)
                },
            )
        }
        val action = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            UpdateInstallActivity.intent(context, downloadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notifications.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.update_download_ready))
                .setContentIntent(action)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .build(),
        )
    }

    fun launchInstaller(activity: Activity, downloadId: Long): Boolean {
        val manager = activity.getSystemService(DownloadManager::class.java)
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return false
        return try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, AppUpdater.APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
            activity.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Copies the DownloadManager content into the app cache, then streams it to PackageManager.
     * Streaming avoids OEM SELinux rules that prevent PackageManager from opening an APK path
     * in /data/local/tmp. Keeping the restart in the same root shell lets it continue even when
     * package replacement kills this process.
     */
    suspend fun silentInstallWithRoot(context: Context, downloadId: Long): Boolean =
        withContext(Dispatchers.IO) {
            if (!claimRootInstall(downloadId)) return@withContext false
            val cachedApk = File(context.cacheDir, "updates/guise-$downloadId.apk")
            try {
                val manager = context.getSystemService(DownloadManager::class.java)
                val descriptor = manager.openDownloadedFile(downloadId)
                cachedApk.parentFile?.mkdirs()
                val expectedSize = descriptor.statSize
                descriptor.use { parcelFile ->
                    FileInputStream(parcelFile.fileDescriptor).use { input ->
                        cachedApk.outputStream().use(input::copyTo)
                    }
                }
                check(cachedApk.length() > 0L)
                check(expectedSize <= 0L || cachedApk.length() == expectedSize)

                val source = shellQuote(cachedApk.absolutePath)
                val component = shellQuote("${context.packageName}/.ui.MainActivity")
                val command = buildString {
                    append("cat $source | ")
                    append("pm install -S ${cachedApk.length()} -r >/dev/null 2>&1; ")
                    append("result=\$?; ")
                    append("if [ \$result -eq 0 ]; then ")
                    append("sleep 1; am start --user current -n $component >/dev/null 2>&1; fi; ")
                    append("exit \$result")
                }
                val process = ProcessBuilder("su", "-c", command)
                    .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                    .redirectOutput(ProcessBuilder.Redirect.to(File("/dev/null")))
                    .redirectError(ProcessBuilder.Redirect.to(File("/dev/null")))
                    .start()
                if (!process.waitFor(ROOT_INSTALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroy()
                    return@withContext false
                }
                process.exitValue() == 0
            } catch (_: Exception) {
                false
            } finally {
                cachedApk.delete()
                releaseRootInstall(downloadId)
            }
        }

    @Synchronized
    private fun claimRootInstall(downloadId: Long): Boolean =
        rootInstallingId.compareAndSet(-1L, downloadId)

    @Synchronized
    private fun releaseRootInstall(downloadId: Long) {
        rootInstallingId.compareAndSet(downloadId, -1L)
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    private val rootInstallingId = AtomicLong(-1L)
}

internal fun readyArtifactMatches(
    expectedVersionCode: Int,
    expectedSha256: String,
    readyVersionCode: Int,
    readySha256: String?,
): Boolean =
    expectedVersionCode > 0 &&
        readyVersionCode == expectedVersionCode &&
        expectedSha256.equals(readySha256.orEmpty(), ignoreCase = true)

class UpdateInstallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val downloadId = intent.getLongExtra(
            EXTRA_DOWNLOAD_ID,
            -1L,
        )
        if (downloadId < 0L) {
            finish()
            return
        }
        setContent {
            GuiseTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = getString(R.string.update_preparing_install),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            try {
                runCatching {
                    val installedWithRoot = UpdateInstaller.silentInstallWithRoot(
                        this@UpdateInstallActivity,
                        downloadId,
                    )
                    if (!installedWithRoot) {
                        UpdateInstaller.launchInstaller(this@UpdateInstallActivity, downloadId)
                    }
                }
            } finally {
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DOWNLOAD_ID = "download_id"

        fun intent(context: Context, downloadId: Long): Intent =
            Intent(context, UpdateInstallActivity::class.java)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        fun open(context: Context, downloadId: Long) {
            context.startActivity(intent(context, downloadId))
        }
    }
}
