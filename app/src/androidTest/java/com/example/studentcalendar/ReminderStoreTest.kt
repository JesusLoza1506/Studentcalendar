package com.example.studentcalendar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.example.studentcalendar.notificaciones.ReminderStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ReminderStoreTest {
    @Test fun reminderSurvivesNewStoreAndRejectsDuplicateOrStaleDelivery() {
        withStore { store ->
            val (event, trigger) = nextAlarm()
            store.reconcile("test-a", listOf(event))
            val reopened = ReminderStore(InstrumentationRegistry.getInstrumentation().targetContext)
            assertNull(reopened.consume("test-b", event.eventId, trigger))
            assertNull(reopened.consume("test-a", event.eventId, trigger - 1000))
            assertEquals(event.nombre, reopened.consume("test-a", event.eventId, trigger)?.optString("name"))
            assertNull(reopened.consume("test-a", event.eventId, trigger))
        }
    }

    @Test fun confirmedDeletionAndSignOutInvalidatePendingReminders() {
        withStore { store ->
            val (event, trigger) = nextAlarm()
            store.reconcile("test-a", listOf(event))
            store.reconcile("test-a", emptyList())
            assertNull(store.consume("test-a", event.eventId, trigger))
            store.reconcile("test-a", listOf(event))
            store.useAccount(null)
            assertNull(store.consume("test-a", event.eventId, trigger))
        }
    }

    private fun withStore(block: (ReminderStore) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val store = ReminderStore(instrumentation.targetContext)
            store.useAccount("test-a")
            try { block(store) } finally { store.useAccount(null) }
        }
    }

    private fun nextAlarm(): Pair<CalendarEvent, Long> {
        val date = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val hour = String.format(Locale.ROOT, "%02d:%02d", date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE))
        val event = CalendarEvent(eventId = "test-alarm", nombre = "Class", fecha = date.time,
            horaInicio = hour, notificacionActivada = true, minutosAntes = 10)
        return event to date.timeInMillis - 600_000L
    }
}
