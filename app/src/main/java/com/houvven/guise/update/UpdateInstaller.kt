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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.houvven.guise.R
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
                    )
                if (verified) {
                    UpdateInstaller.markReady(context, completed)
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

    fun markReady(context: Context, downloadId: Long) {
        context.getSharedPreferences(AppUpdater.UPDATE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putLong(AppUpdater.KEY_READY_DOWNLOAD_ID, downloadId)
            .apply()
    }

    fun pending(context: Context): Long =
        context.getSharedPreferences(AppUpdater.UPDATE_PREFERENCES, Context.MODE_PRIVATE)
            .getLong(AppUpdater.KEY_READY_DOWNLOAD_ID, -1L)

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
    ): Boolean = withContext(Dispatchers.IO) {
        if (expectedSha256.isBlank()) return@withContext true
        val manager = context.getSystemService(DownloadManager::class.java)
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            manager.openDownloadedFile(downloadId).use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
            }
            digest.digest().joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }.equals(expectedSha256, ignoreCase = true)
        }.getOrDefault(false)
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

class UpdateInstallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val downloadId = intent.getLongExtra(
            EXTRA_DOWNLOAD_ID,
            UpdateInstaller.pending(this),
        )
        if (downloadId < 0L) {
            finish()
            return
        }
        lifecycleScope.launch {
            val installedWithRoot = UpdateInstaller.silentInstallWithRoot(
                this@UpdateInstallActivity,
                downloadId,
            )
            if (!installedWithRoot) {
                UpdateInstaller.launchInstaller(this@UpdateInstallActivity, downloadId)
            }
            finish()
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
