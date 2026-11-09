package com.example.studentcalendar.eventos

import java.util.Calendar
import java.util.Date

/** Shared calendar rules for event forms, reminders and the schedule. */
object EventSchedule {
    // Editing can delete and create a full series in the same Firestore batch.
    const val MAX_WEEKS = 26

    fun minutes(value: String): Int? {
        val parts = value.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return (hour * 60 + minute).takeIf { hour in 0..23 && minute in 0..59 }
    }

    fun atTime(date: Date, time: String): Date? {
        val minuteOfDay = minutes(time) ?: return null
        return Calendar.getInstance().apply {
            this.time = date
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    fun occurrences(from: Date, time: String, days: Collection<Int>, weeks: Int): List<Date> {
        if (weeks !in 1..MAX_WEEKS) return emptyList()
        val start = atTime(from, "00:00") ?: return emptyList()
        val day = Calendar.getInstance().apply { this.time = start }
        val daysFromMonday = (day.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return buildList {
            repeat(weeks * 7 - daysFromMonday) {
                if (day.get(Calendar.DAY_OF_WEEK) in days) {
                    atTime(day.time, time)?.let { add(it) }
                }
                day.add(Calendar.DATE, 1)
            }
        }
    }

    fun reminderAt(date: Date, time: String, leadMinutes: Int?, now: Long): Long? {
        if (leadMinutes == null || leadMinutes < 0) return null
        val start = atTime(date, time) ?: return null
        return (start.time - leadMinutes * 60_000L).takeIf { it > now }
    }

    fun visibleBetween(start: Int, end: Int?, from: Int, to: Int): Boolean {
        return if (end == null) start in from..to else start < to && end > from && end > start
    }

    fun shiftedSeriesStart(first: Date, original: Date, selected: Date): Date {
        val originalDay = Calendar.getInstance().apply { time = atTime(original, "00:00")!! }
        val selectedDay = Calendar.getInstance().apply { time = atTime(selected, "00:00")!! }
        // Use UTC for the date difference to avoid 23/25-hour days in local time.
        fun dayNumber(value: Calendar): Long = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DATE))
        }.timeInMillis / 86_400_000
        val delta = (dayNumber(selectedDay) - dayNumber(originalDay)).toInt()
        return Calendar.getInstance().apply {
            time = first
            add(Calendar.DATE, delta)
        }.time
    }
}
