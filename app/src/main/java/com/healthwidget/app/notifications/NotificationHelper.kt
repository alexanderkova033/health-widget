package com.healthwidget.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.healthwidget.app.R
import com.healthwidget.app.ui.MainActivity

/**
 * All notification plumbing in one place: channel setup and posting. Every post is gated on
 * POST_NOTIFICATIONS (required at runtime on API 33+); if it's not granted we simply skip
 * showing the notification rather than nagging the user to grant it — consistent with the
 * app's zero-friction, zero-guilt philosophy.
 */
object NotificationHelper {
    const val NUDGE_CHANNEL_ID = "nudge_tips"
    const val SLEEP_CHANNEL_ID = "sleep_alert"

    private const val NUDGE_NOTIFICATION_ID = 100
    private const val SLEEP_NOTIFICATION_ID = 200

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NUDGE_CHANNEL_ID,
                context.getString(R.string.channel_nudge_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_nudge_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                SLEEP_CHANNEL_ID,
                context.getString(R.string.channel_sleep_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_sleep_description)
            },
        )
    }

    fun showNudge(
        context: Context,
        tip: String,
    ) = show(context, NUDGE_CHANNEL_ID, NUDGE_NOTIFICATION_ID, tip)

    fun showSleepAlert(
        context: Context,
        message: String,
    ) = show(context, SLEEP_CHANNEL_ID, SLEEP_NOTIFICATION_ID, message)

    private fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        text: String,
    ) {
        // Checked inline (rather than via a wrapper function) so lint's MissingPermission
        // data-flow check can recognize the guard on the notify() call below.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
