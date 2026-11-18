package com.isora.ciclo.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.isora.ciclo.CicloApplication
import com.isora.ciclo.MainActivity
import com.isora.ciclo.R
import com.isora.ciclo.agenda.CareSnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CHANNEL_ID = "friday_summary"
private const val NOTIFICATION_ID = 1
private const val REQUEST_CODE = 100

private val dayFormat = DateTimeFormatter.ofPattern("EEEE d", Locale("es"))

/** Schedules the weekly alarm and posts the summary when it rings. */
object FridayNotifier {

    /** Arms (or cancels) the next Friday alarm according to the settings. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        val container = (app as CicloApplication).container
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = alarmIntent(app)
        if (!container.settings.fridayEnabled) {
            alarms.cancel(pending)
            return
        }
        val at = nextFriday(LocalDateTime.now(), container.settings.fridayTime)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (canBeExact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    fun post(context: Context, snapshot: CareSnapshot, friday: LocalDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val lines = summaryLines(context, buildFridaySummary(snapshot, friday))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        // The lock screen shows a generic text: patient names appear only after unlocking the phone.
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notification_title))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun summaryLines(context: Context, summary: FridaySummary): List<String> {
        if (summary.isEmpty) return listOf(context.getString(R.string.notification_empty))
        val lines = mutableListOf<String>()
        summary.sessionsByDay.forEach { (date, sessions) ->
            val names = sessions.joinToString(", ") { it.entry.name }
            lines += "${dayFormat.format(date).replaceFirstChar { it.uppercase() }}: $names"
        }
        if (summary.postponed.isNotEmpty()) {
            lines += context.getString(R.string.notification_postponed, summary.postponed.joinToString(", ") { it.entry.name })
        }
        if (summary.finishing.isNotEmpty()) {
            lines += context.getString(R.string.notification_finishing, summary.finishing.joinToString(", ") { it.name })
        }
        if (summary.reevaluate.isNotEmpty()) {
            lines += context.getString(R.string.notification_reevaluate, summary.reevaluate.joinToString(", ") { it.name })
        }
        return lines
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, FridayReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Rings on Friday: posts the summary of next week and arms the following Friday. */
class FridayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val container = (context.applicationContext as CicloApplication).container
                val snapshot = CareSnapshot(container.patients.all(), container.treatments.all())
                FridayNotifier.post(context.applicationContext, snapshot, LocalDate.now())
            } finally {
                FridayNotifier.schedule(context.applicationContext)
                pending.finish()
            }
        }.start()
    }
}

/** Alarms do not survive a reboot, an app update or a clock change; arm the next Friday again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FridayNotifier.schedule(context.applicationContext)
    }
}
