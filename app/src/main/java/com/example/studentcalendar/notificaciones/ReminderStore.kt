package com.example.studentcalendar.notificaciones

import android.content.Context
import com.example.studentcalendar.eventos.EventSchedule
import com.example.studentcalendar.horariosemanal.CalendarEvent
import org.json.JSONObject

/** Stores the last confirmed reminders on this device, including across an offline reboot. */
class ReminderStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("confirmed_reminders", Context.MODE_PRIVATE)

    fun useAccount(uid: String?) {
        if (preferences.getString("owner", null) == uid) return
        val oldEntries = entries()
        oldEntries.keys().forEach { NotificationHelper.cancel(context, it) }
        context.stopService(android.content.Intent(context, AlarmService::class.java))
        preferences.edit().clear().putString("owner", uid).commit()
    }

    fun reconcile(uid: String, events: List<CalendarEvent>) {
        if (preferences.getString("owner", null) != uid) return
        val previous = entries()
        val next = JSONObject()
        val now = System.currentTimeMillis()
        for (event in events) {
            if (!event.activo || !event.notificacionActivada) continue
            val trigger = EventSchedule.reminderAt(event.fecha, event.horaInicio, event.minutosAntes, Long.MIN_VALUE)
                ?: continue
            if (trigger <= now && previous.optJSONObject(event.eventId)?.optLong("trigger") != trigger) continue
            next.put(event.eventId, JSONObject().apply {
                put("trigger", trigger)
                put("name", event.nombre)
                put("type", event.tipo)
                put("room", event.aula)
                put("sound", event.sonidoAlarmaActivado)
                put("uri", event.uriSonidoAlarma.orEmpty())
                put("custom", event.esSonidoPersonalizado)
                put("duration", event.duracionSonidoPersonalizado ?: 0L)
                put("vibration", event.vibracionActivada)
                put("pattern", event.patronVibracion.orEmpty())
            })
        }

        if (!preferences.edit().putString("entries", next.toString()).commit()) return
        previous.keys().forEach { if (!next.has(it)) NotificationHelper.cancel(context, it) }
        restore(uid)
    }

    fun remove(uid: String, eventIds: Iterable<String>) {
        if (preferences.getString("owner", null) != uid) return
        val saved = entries()
        eventIds.forEach {
            saved.remove(it)
            NotificationHelper.cancel(context, it)
        }
        preferences.edit().putString("entries", saved.toString()).commit()
    }

    fun restore(uid: String) {
        if (preferences.getString("owner", null) != uid) return
        val saved = entries()
        saved.keys().forEach { id -> NotificationHelper.schedule(context, id, saved.getJSONObject(id).getLong("trigger")) }
    }

    fun consume(uid: String, eventId: String, trigger: Long): JSONObject? {
        if (preferences.getString("owner", null) != uid) return null
        val saved = entries()
        val alarm = saved.optJSONObject(eventId) ?: return null
        if (alarm.optLong("trigger") != trigger) return null
        saved.remove(eventId)
        if (!preferences.edit().putString("entries", saved.toString()).commit()) return null
        return alarm
    }

    private fun entries(): JSONObject =
        runCatching { JSONObject(preferences.getString("entries", "{}") ?: "{}") }.getOrElse { JSONObject() }
}
