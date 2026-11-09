package com.example.studentcalendar

import com.example.studentcalendar.eventos.EventSchedule
import com.example.studentcalendar.horariosemanal.CalendarEvent
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

class EventScheduleTest {
    private fun date(day: Int, hour: Int = 0, minute: Int = 0) =
        GregorianCalendar(2026, Calendar.NOVEMBER, day, hour, minute).time

    @Test fun rejectsMalformedHours() {
        listOf("", "11", "24:00", "09:60", "ab:10", "-1:00").forEach {
            assertNull(EventSchedule.minutes(it))
        }
        assertEquals(1439, EventSchedule.minutes("23:59"))
    }

    @Test fun classCreationStartsOnOrAfterSelectedDayAndKeepsSundays() {
        assertEquals(
            listOf(date(15, 19, 30), date(17, 19, 30), date(22, 19, 30)),
            EventSchedule.occurrences(date(12), "19:30", listOf(Calendar.TUESDAY, Calendar.SUNDAY), 2)
        )
    }

    @Test fun classCreationRejectsZeroAndExcessiveWeeks() {
        for (weeks in listOf(0, -1, EventSchedule.MAX_WEEKS + 1)) {
            assertTrue(EventSchedule.occurrences(date(9), "08:00", listOf(Calendar.MONDAY), weeks).isEmpty())
        }
        assertTrue(EventSchedule.occurrences(date(9), "08:00", emptyList(), 4).isEmpty())
    }

    @Test fun editingLaterOccurrenceKeepsSeriesStartWhenDateIsUnchanged() {
        assertEquals(date(9, 17), EventSchedule.shiftedSeriesStart(date(9, 17), date(23, 17), date(23, 19)))
        assertEquals(date(10, 17), EventSchedule.shiftedSeriesStart(date(9, 17), date(23, 17), date(24, 19)))
    }

    @Test fun oldEventsDoNotAcquireAnImplicitSeries() {
        assertTrue(CalendarEvent(nombre = "Algorithms").seriesId.isBlank())
    }

    @Test fun taskDeadlineUsesSelectedDayAndExactMinutes() {
        assertEquals(date(12, 14, 5), EventSchedule.atTime(date(12, 8), "14:05"))
    }

    @Test fun reminderIsComputedFromEventTimeAndAdvance() {
        assertEquals(date(12, 9, 45).time, EventSchedule.reminderAt(date(12), "10:00", 15, date(12, 9).time))
        assertEquals(date(11, 23, 55).time, EventSchedule.reminderAt(date(12), "00:05", 10, date(11).time))
    }

    @Test fun rejectsExpiredOrInvalidReminders() {
        val now = date(12, 9, 45).time
        assertNull(EventSchedule.reminderAt(date(12), "10:00", 15, now))
        assertNull(EventSchedule.reminderAt(Date(), "bad", 5, now))
        assertNull(EventSchedule.reminderAt(date(13), "10:00", null, now))
        assertNull(EventSchedule.reminderAt(date(13), "10:00", -5, now))
    }

    @Test fun scheduleIncludesOverlappingClassesAndPointInTimeTasks() {
        assertTrue(EventSchedule.visibleBetween(8 * 60, 10 * 60, 9 * 60, 11 * 60))
        assertFalse(EventSchedule.visibleBetween(8 * 60, 9 * 60, 9 * 60, 11 * 60))
        assertTrue(EventSchedule.visibleBetween(10 * 60, null, 9 * 60, 11 * 60))
        assertFalse(EventSchedule.visibleBetween(12 * 60, null, 9 * 60, 11 * 60))
    }
}
