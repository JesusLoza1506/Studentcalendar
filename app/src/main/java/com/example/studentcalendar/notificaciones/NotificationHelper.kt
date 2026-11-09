package com.example.studentcalendar.notificaciones

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.studentcalendar.R

object NotificationHelper {
    private fun manager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun ensureExactPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager(activity).canScheduleExactAlarms()) return true
        Toast.makeText(activity, R.string.toast_enable_exact_alarms_permission, Toast.LENGTH_LONG).show()
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${activity.packageName}")))
        }
        return false
    }

    internal fun schedule(context: Context, eventId: String, trigger: Long) {
        if (trigger <= System.currentTimeMillis()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager(context).canScheduleExactAlarms()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val pending = pendingIntent(context, eventId, trigger, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            manager(context).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } catch (exception: SecurityException) {
            Log.w("KalendUAlarm", "Exact alarm access changed before scheduling", exception)
        }
    }

    internal fun cancel(context: Context, eventId: String) {
        pendingIntent(context, eventId, 0L, PendingIntent.FLAG_NO_CREATE)?.let {
            manager(context).cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(context: Context, eventId: String, trigger: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            data = Uri.parse("custom://$eventId")
            putExtra("EVENT_ID", eventId)
            putExtra("TRIGGER_AT", trigger)
        }
        return PendingIntent.getBroadcast(context, eventId.hashCode(), intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }
}
