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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.houvven.guise.R

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val expected = context.getSharedPreferences(
            AppUpdater.UPDATE_PREFERENCES,
            Context.MODE_PRIVATE,
        ).getLong(AppUpdater.KEY_DOWNLOAD_ID, -1L)
        val completed = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (expected < 0L || completed != expected) return
        if (!UpdateInstaller.isSuccessful(context, completed)) return

        UpdateInstaller.markReady(context, completed)
        UpdateInstaller.showInstallNotification(context, completed)
    }
}

object UpdateInstaller {
    private const val CHANNEL_ID = "app_update_install"
    private const val NOTIFICATION_ID = 0x4755

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
}

class UpdateInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val downloadId = intent.getLongExtra(
            EXTRA_DOWNLOAD_ID,
            UpdateInstaller.pending(this),
        )
        if (downloadId >= 0L) {
            UpdateInstaller.launchInstaller(this, downloadId)
        }
        finish()
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
