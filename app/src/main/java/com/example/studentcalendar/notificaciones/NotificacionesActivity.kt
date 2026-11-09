package com.example.studentcalendar.notificaciones

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import com.example.studentcalendar.eventos.EventSchedule
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
// Ya no necesitamos importar AppCompatActivity directamente
// import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.R
import com.example.studentcalendar.databinding.ActivityNotificacionesBinding
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.ajustes.ThemeHelper
import java.util.*
import android.util.Log
import androidx.cardview.widget.CardView

import com.example.studentcalendar.notificaciones.NotificationHelper
import com.example.studentcalendar.vertareas.EventConstants
import com.example.studentcalendar.base.BaseActivity

class NotificacionesActivity : BaseActivity<ActivityNotificacionesBinding>() {

    override fun getViewBinding(): ActivityNotificacionesBinding {
        return ActivityNotificacionesBinding.inflate(layoutInflater)
    }

    override fun getNavigationMenuItemId(): Int = R.id.nav_notifications


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("PreferenciasKalendU", Context.MODE_PRIVATE)

        binding.tituloNotificaciones.text = getString(R.string.title_active_reminders_notifications)
        binding.textoExplicativo.text = getString(R.string.explanation_reminders_notifications)
    }

    override fun onResume() {
        super.onResume()
        cargarEventos()
        Log.d("NotificacionesActivity", "onResume: Recargando eventos.")
    }

    private fun cargarEventos() {
        val user = auth.currentUser ?: run {
            Log.w("NotificacionesActivity", "Usuario no autenticado al cargar eventos.")
            mostrarMensajeSinRecordatorios(binding.contenedorRecordatorios)
            return
        }
        val contenedor = binding.contenedorRecordatorios
        contenedor.removeAllViews()

        db.collection("users")
            .document(user.uid)
            .collection("events")
            .get()
            .addOnSuccessListener { docs ->
                val now = System.currentTimeMillis()
                val activeReminders = docs.mapNotNull { doc ->
                    doc.toObject(CalendarEvent::class.java)?.copy(eventId = doc.id)
                }.filter {
                    it.activo && it.notificacionActivada &&
                        EventSchedule.reminderAt(it.fecha, it.horaInicio, it.minutosAntes, now) != null
                }

                if (activeReminders.isEmpty()) {
                    mostrarMensajeSinRecordatorios(contenedor)
                    return@addOnSuccessListener
                }

                val sortedReminders = activeReminders.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))

                for (evento in sortedReminders) {
                    mostrarEvento(evento, contenedor)
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificacionesActivity", "Error al cargar eventos para recordatorios: ${e.message}", e)
                Toast.makeText(this, getString(R.string.error_loading_events_notifications), Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarEvento(evento: CalendarEvent, contenedor: LinearLayout) {
        val recordatorioView = LayoutInflater.from(this).inflate(R.layout.item_recordatorio, contenedor, false) as CardView

        val tvReminderTitle: TextView = recordatorioView.findViewById(R.id.tvReminderTitle)
        val tvTime: TextView = recordatorioView.findViewById(R.id.tvTime)
        val tvDays: TextView = recordatorioView.findViewById(R.id.tvDays)
        val tvDate: TextView = recordatorioView.findViewById(R.id.tvDate)
        val tvMinutesBefore: TextView = recordatorioView.findViewById(R.id.tvMinutesBefore)
        val btnModify: Button = recordatorioView.findViewById(R.id.btnModify)
        val btnDelete: Button = recordatorioView.findViewById(R.id.btnDelete)
        val tvReminderIcon: TextView = recordatorioView.findViewById(R.id.tvReminderIcon)

        tvReminderTitle.text = when (evento.tipo) {
            EventConstants.TYPE_CLASS -> getString(R.string.event_type_class_prefix, evento.nombre)
            EventConstants.TYPE_TASK -> getString(R.string.event_type_task_prefix, evento.nombre)
            else -> evento.nombre
        }

        tvTime.text = if (evento.tipo == EventConstants.TYPE_CLASS) {
            getString(R.string.time_range_format, evento.horaInicio, evento.horaFin)
        } else {
            getString(R.string.time_single_format, evento.horaInicio)
        }

        val diasSemanaTexto = if (evento.tipo == EventConstants.TYPE_CLASS && evento.diasSemana.isNotEmpty()) {
            evento.diasSemana.joinToString(", ") { dayNum ->
                when (dayNum) {
                    Calendar.MONDAY -> getString(R.string.day_monday_full)
                    Calendar.TUESDAY -> getString(R.string.day_tuesday_full)
                    Calendar.WEDNESDAY -> getString(R.string.day_wednesday_full)
                    Calendar.THURSDAY -> getString(R.string.day_thursday_full)
                    Calendar.FRIDAY -> getString(R.string.day_friday_full)
                    Calendar.SATURDAY -> getString(R.string.day_saturday_full)
                    Calendar.SUNDAY -> getString(R.string.day_sunday_full)
                    else -> ""
                }
            }
        } else {
            getString(R.string.not_applicable)
        }
        tvDays.text = getString(R.string.days_label_format, diasSemanaTexto)

        val fechaFormateada = evento.fecha?.let {
            DateFormat.format("dd/MM/yyyy", it).toString()
        } ?: getString(R.string.no_date_available)
        tvDate.text = getString(R.string.date_label_format, fechaFormateada)

        tvMinutesBefore.text = getString(R.string.minutes_before_label_format, evento.minutosAntes ?: 0)

        btnModify.setOnClickListener {
            mostrarDialogoModificarMinutos(evento)
        }

        btnDelete.setOnClickListener {
            confirmarEliminacion(evento)
        }

        contenedor.addView(recordatorioView)
    }

    private fun mostrarDialogoModificarMinutos(evento: CalendarEvent) {
        val reminderOptionsArray = resources.getStringArray(R.array.reminder_options)
        val opcionesMinutos = reminderOptionsArray.map {
            it.split(" ")[0].toInt()
        }.toTypedArray()

        val actual = opcionesMinutos.indexOf(evento.minutosAntes ?: 5)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_how_many_minutes))
            .setSingleChoiceItems(opcionesMinutos.map { it.toString() }.toTypedArray(), actual) { dialog, which ->
                val nuevosMinutos = opcionesMinutos[which]
                if (EventSchedule.reminderAt(evento.fecha, evento.horaInicio, nuevosMinutos, System.currentTimeMillis()) == null) {
                    Toast.makeText(this, R.string.reminder_time_passed, Toast.LENGTH_LONG).show()
                    return@setSingleChoiceItems
                }
                if (!NotificationHelper.ensureExactPermission(this)) {
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }

                val user = auth.currentUser ?: return@setSingleChoiceItems
                val eventoRef = db.collection("users")
                    .document(user.uid)
                    .collection("events")
                    .document(evento.eventId)

                val updates = mapOf(
                    "minutosAntes" to nuevosMinutos,
                    "notificacionActivada" to true
                )

                eventoRef.update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.notification_updated_success, nuevosMinutos), Toast.LENGTH_SHORT).show()
                        cargarEventos()
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificacionesActivity", "Error al actualizar notificación: ${e.message}", e)
                        Toast.makeText(this, getString(R.string.error_updating_notification), Toast.LENGTH_SHORT).show()
                    }

                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun confirmarEliminacion(evento: CalendarEvent) {
        val fechaFormateada = evento.fecha?.let {
            DateFormat.format("dd/MM/yyyy", it).toString()
        } ?: getString(R.string.no_date_available)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_delete_reminder))
            .setMessage(getString(R.string.dialog_message_confirm_delete_reminder, evento.nombre, fechaFormateada))
            .setPositiveButton(getString(R.string.yes_delete_button)) { _, _ ->
                eliminarRecordatorio(evento)
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun eliminarRecordatorio(evento: CalendarEvent) {
        val user = auth.currentUser ?: return
        val eventoRef = db.collection("users")
            .document(user.uid)
            .collection("events")
            .document(evento.eventId)

        val updates = mapOf(
            "minutosAntes" to null,
            "notificacionActivada" to false
        )

        eventoRef.update(updates)
            .addOnSuccessListener {
                ReminderStore(this).remove(user.uid, listOf(evento.eventId))
                Toast.makeText(this, getString(R.string.reminder_deleted_success), Toast.LENGTH_SHORT).show()
                cargarEventos()
            }
            .addOnFailureListener { e ->
                Log.e("NotificacionesActivity", "Error al eliminar recordatorio: ${e.message}", e)
                Toast.makeText(this, getString(R.string.error_deleting_reminder), Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarMensajeSinRecordatorios(contenedor: LinearLayout) {
        contenedor.removeAllViews()
        val txt = TextView(this).apply {
            text = getString(R.string.no_active_reminders)
            setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(10, 20, 10, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
        }
        contenedor.addView(txt)
    }
}