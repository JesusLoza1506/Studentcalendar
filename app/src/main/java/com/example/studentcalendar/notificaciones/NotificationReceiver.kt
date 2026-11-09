package com.example.studentcalendar.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val id = intent.getStringExtra("EVENT_ID") ?: return
        val trigger = intent.getLongExtra("TRIGGER_AT", 0L)
        if (trigger <= 0 || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val event = ReminderStore(context).consume(uid, id, trigger) ?: return
        val service = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_EVENT_ID, id)
            putExtra(AlarmService.EXTRA_EVENT_NAME, event.optString("name"))
            putExtra(AlarmService.EXTRA_EVENT_TYPE, event.optString("type"))
            putExtra(AlarmService.EXTRA_EVENT_CLASSROOM, event.optString("room"))
            putExtra(AlarmService.EXTRA_SOUND_ENABLED, event.optBoolean("sound"))
            putExtra(AlarmService.EXTRA_SOUND_URI, event.optString("uri"))
            putExtra(AlarmService.EXTRA_IS_CUSTOM_SOUND, event.optBoolean("custom"))
            putExtra(AlarmService.EXTRA_CUSTOM_SOUND_DURATION, event.optLong("duration"))
            putExtra(AlarmService.EXTRA_VIBRATION_ENABLED, event.optBoolean("vibration"))
            putExtra(AlarmService.EXTRA_VIBRATION_PATTERN_NAME, event.optString("pattern"))
        }
        runCatching { ContextCompat.startForegroundService(context, service) }
            .onFailure { Log.w("KalendUAlarm", "Could not start alarm service", it) }
    }
}

class RestoreRemindersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) return
        FirebaseAuth.getInstance().currentUser?.uid?.let { ReminderStore(context).restore(it) }
    }
}
