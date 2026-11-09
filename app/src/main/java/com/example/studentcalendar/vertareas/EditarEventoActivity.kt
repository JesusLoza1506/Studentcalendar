package com.example.studentcalendar.vertareas

import com.example.studentcalendar.notificaciones.ReminderStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.databinding.ActivityEditarEventoBinding
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.ajustes.ThemeHelper
import com.example.studentcalendar.R
import com.example.studentcalendar.eventos.EventSchedule
import com.google.firebase.firestore.DocumentSnapshot

class EditarEventoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarEventoBinding
    private lateinit var evento: CalendarEvent
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val calendario = Calendar.getInstance()
    private var saving = false
    private var originalDays = emptyList<Int>()
    private val editsSeries get() = evento.tipo == EventConstants.TYPE_CLASS && evento.seriesId.isNotBlank()

    private val coloresDisponibles = listOf(
        "#3498DB", "#E74C3C", "#2ECC71", "#9B59B6", "#F1C40F",
        "#1ABC9C", "#E67E22", "#34495E", "#95A5A6", "#7D3C98",
        "#FF69B4", "#8A2BE2", "#20B2AA", "#FFD700", "#6A5ACD"
    )

    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.aplicarTema(this)
        super.onCreate(savedInstanceState)
        binding = ActivityEditarEventoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        establecerTextos()

        val eventoId = intent.getStringExtra("eventoId") ?: run {
            Toast.makeText(this, getString(R.string.error_loading_event), Toast.LENGTH_SHORT).show()
            return finish()
        }
        setBusy(true)
        cargarEvento(eventoId)

        configurarListeners()
    }

    private fun establecerTextos() {
        binding.tvEditEventTitle.text = getString(R.string.edit_event_title)
        binding.etNombre.hint = getString(R.string.hint_event_name_edit)
        binding.etAula.hint = getString(R.string.hint_classroom_optional_edit)
        binding.tvTipo.text = getString(R.string.label_event_type_edit)
        binding.tvFecha.text = getString(R.string.label_date_edit)
        binding.tvHoraInicio.text = getString(R.string.label_start_time_edit)
        binding.tvHoraFin.text = getString(R.string.label_end_time_edit)
        binding.tvColor.text = getString(R.string.button_color_edit)
        binding.tvDiasSemana.text = getString(R.string.label_days_of_week_class)
        binding.etRepeticion.hint = getString(R.string.hint_repetition_weeks)
        binding.btnGuardarCambios.text = getString(R.string.button_save_changes)
        binding.btnEliminarEvento.text = getString(R.string.button_delete_event)
    }

    private fun configurarListeners() {
        binding.tvFecha.setOnClickListener { mostrarSelectorFecha() }
        binding.tvHoraInicio.setOnClickListener { mostrarSelectorHora(binding.tvHoraInicio) }
        binding.tvHoraFin.setOnClickListener { mostrarSelectorHora(binding.tvHoraFin) }
        binding.tvColor.setOnClickListener { mostrarSelectorColor() }
        binding.tvDiasSemana.setOnClickListener { manejarClickSelectorDia() }
        binding.btnGuardarCambios.setOnClickListener { guardarCambios(evento.eventId) }
        binding.btnEliminarEvento.setOnClickListener { mostrarDialogoConfirmacionEliminar() }
    }

    private fun manejarClickSelectorDia() {
        if (::evento.isInitialized && editsSeries) {
            mostrarSelectorDiasSemana()
        } else {
            Toast.makeText(this, getString(R.string.option_only_for_classes), Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarEvento(eventoId: String) {
        val userId = auth.currentUser?.uid ?: return finish()

        db.collection("users").document(userId).collection("events").document(eventoId)
            .get()
            .addOnSuccessListener { doc ->
                evento = doc.toObject(CalendarEvent::class.java) ?: run {
                    Toast.makeText(this, getString(R.string.error_loading_event), Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                evento.eventId = doc.id
                originalDays = evento.diasSemana.toList()
                calendario.time = evento.fecha
                actualizarUIConDatosEvento()
                setBusy(false)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_loading_event), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun actualizarUIConDatosEvento() {
        binding.etNombre.setText(evento.nombre)
        binding.etAula.setText(evento.aula)
        binding.tvFecha.text = formatoFecha.format(evento.fecha)
        binding.tvHoraInicio.text = evento.horaInicio
        binding.tvHoraFin.text = evento.horaFin
        val eventColor = runCatching { Color.parseColor(evento.color) }.getOrDefault(Color.GRAY)
        binding.tvColor.text = String.format(Locale.ROOT, "#%06X", 0xFFFFFF and eventColor)
        binding.tvColor.setBackgroundColor(eventColor)
        actualizarContrasteTextoBotonColor(eventColor)

        binding.tvTipo.text = when (evento.tipo) {
            EventConstants.TYPE_CLASS -> getString(R.string.event_type_class)
            EventConstants.TYPE_TASK -> getString(R.string.event_type_task)
            else -> evento.tipo
        }

        val esClase = evento.tipo == EventConstants.TYPE_CLASS

        binding.tvDiasSemana.visibility = if (editsSeries) View.VISIBLE else View.GONE
        binding.etRepeticion.visibility = if (editsSeries) View.VISIBLE else View.GONE
        binding.tvHoraFin.visibility = if (esClase) View.VISIBLE else View.GONE

        if (esClase && evento.diasSemana.isNotEmpty()) {
            val nombres = mapOf(
                Calendar.MONDAY to getString(R.string.day_monday_full),
                Calendar.TUESDAY to getString(R.string.day_tuesday_full),
                Calendar.WEDNESDAY to getString(R.string.day_wednesday_full),
                Calendar.THURSDAY to getString(R.string.day_thursday_full),
                Calendar.FRIDAY to getString(R.string.day_friday_full),
                Calendar.SATURDAY to getString(R.string.day_saturday_full),
                Calendar.SUNDAY to getString(R.string.day_sunday_full)
            )
            binding.tvDiasSemana.text = evento.diasSemana.sorted().joinToString(", ") { nombres[it] ?: "" }
        } else {
            binding.tvDiasSemana.text = getString(R.string.not_applicable)
        }

        if (esClase) {
            binding.etRepeticion.setText(if (evento.repeticiones > 0) evento.repeticiones.toString() else "")
        } else {
            binding.etRepeticion.setText("")
        }
    }

    private fun mostrarSelectorFecha() {
        val cal = Calendar.getInstance().apply {
            time = evento.fecha
        }
        DatePickerDialog(this, { _, y, m, d ->
            calendario.set(y, m, d)
            binding.tvFecha.text = formatoFecha.format(calendario.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun mostrarSelectorHora(vista: TextView) {
        val (hora, minuto) = (vista.text.toString().split(":").map { it.toIntOrNull() }).let {
            Pair(it.getOrNull(0) ?: 8, it.getOrNull(1) ?: 0)
        }

        TimePickerDialog(this, { _, h, m ->
            vista.text = String.format("%02d:%02d", h, m)
        }, hora, minuto, true).show()
    }

    private fun mostrarSelectorColor() {
        val layout = layoutInflater.inflate(R.layout.dialog_color_selector, null)
        val grid = layout.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.colorGrid)
        grid.removeAllViews()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_color_title)
            .setView(layout)
            .setNegativeButton(R.string.cancel_action, null)
            .create()

        coloresDisponibles.forEach { colorHex ->
            val box = View(this).apply {
                setBackgroundColor(Color.parseColor(colorHex))
                val size = resources.getDimensionPixelSize(R.dimen.color_box_size)
                val margin = resources.getDimensionPixelSize(R.dimen.color_box_margin)
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setOnClickListener {
                    binding.tvColor.text = colorHex
                    binding.tvColor.setBackgroundColor(Color.parseColor(colorHex))
                    actualizarContrasteTextoBotonColor(Color.parseColor(colorHex))
                    dialog.dismiss()
                }
            }
            grid.addView(box)
        }

        dialog.show()
    }

    private fun actualizarContrasteTextoBotonColor(color: Int) {
        val brightness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        binding.tvColor.setTextColor(if (brightness >= 0.5) Color.WHITE else Color.BLACK)
    }

    private fun mostrarSelectorDiasSemana() {
        val diasNombres = arrayOf(
            getString(R.string.day_monday_full),
            getString(R.string.day_tuesday_full),
            getString(R.string.day_wednesday_full),
            getString(R.string.day_thursday_full),
            getString(R.string.day_friday_full),
            getString(R.string.day_saturday_full),
            getString(R.string.day_sunday_full)
        )

        val indexToCalendarDay = mapOf(
            0 to Calendar.MONDAY,
            1 to Calendar.TUESDAY,
            2 to Calendar.WEDNESDAY,
            3 to Calendar.THURSDAY,
            4 to Calendar.FRIDAY,
            5 to Calendar.SATURDAY,
            6 to Calendar.SUNDAY
        )

        val seleccionados = BooleanArray(7) { i ->
            val calendarDay = indexToCalendarDay[i] ?: -1
            evento.diasSemana.contains(calendarDay)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_days_dialog_title)
            .setMultiChoiceItems(diasNombres, seleccionados) { _, i, checked ->
                seleccionados[i] = checked
            }
            .setPositiveButton(R.string.accept_action) { _, _ ->
                val result = mutableListOf<Int>()
                val nombresParaUI = mutableListOf<String>()

                for (i in seleccionados.indices) {
                    if (seleccionados[i]) {
                        val calendarDayValue = indexToCalendarDay[i]
                        if (calendarDayValue != null) {
                            result.add(calendarDayValue)
                            nombresParaUI.add(diasNombres[i])
                        }
                    }
                }
                evento.diasSemana = result.sorted()
                binding.tvDiasSemana.text = nombresParaUI.joinToString(", ")
            }
            .setNegativeButton(R.string.cancel_action, null)
            .show()
    }

    private fun setBusy(value: Boolean) {
        saving = value
        binding.btnGuardarCambios.isEnabled = !value
        binding.btnEliminarEvento.isEnabled = !value
    }

    private fun guardarCambios(eventoId: String) {
        if (saving) return
        val uid = auth.currentUser?.uid ?: return
        val name = binding.etNombre.text.toString().trim()
        val start = binding.tvHoraInicio.text.toString().trim()
        val end = binding.tvHoraFin.text.toString().trim()
        val startMinutes = EventSchedule.minutes(start)
        val endMinutes = EventSchedule.minutes(end)
        val date = runCatching { formatoFecha.parse(binding.tvFecha.text.toString()) }.getOrNull()
            ?.let { EventSchedule.atTime(it, start) }
        if (name.isEmpty() || startMinutes == null || date == null || !date.after(Date())) {
            Toast.makeText(this, R.string.invalid_future_event, Toast.LENGTH_LONG).show()
            return
        }
        if (evento.tipo == EventConstants.TYPE_CLASS && (endMinutes == null || endMinutes <= startMinutes)) {
            Toast.makeText(this, R.string.toast_start_time_after_end_time, Toast.LENGTH_LONG).show()
            return
        }

        val fields = mapOf(
            "nombre" to name,
            "aula" to binding.etAula.text.toString().trim(),
            "fecha" to date,
            "horaInicio" to start,
            "horaFin" to if (evento.tipo == EventConstants.TYPE_CLASS) end else "",
            "color" to binding.tvColor.text.toString().trim()
        )
        val events = db.collection("users").document(uid).collection("events")
        if (!editsSeries) {
            setBusy(true)
            events.document(eventoId).update(fields)
                .addOnSuccessListener { finish() }
                .addOnFailureListener { showWriteError(it) }
            return
        }

        val weeks = binding.etRepeticion.text.toString().toIntOrNull()
        if (weeks == null || weeks !in 1..EventSchedule.MAX_WEEKS || evento.diasSemana.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_series, EventSchedule.MAX_WEEKS), Toast.LENGTH_LONG).show()
            return
        }

        setBusy(true)
        events.whereEqualTo("seriesId", evento.seriesId).get()
            .addOnSuccessListener { result ->
                val now = Date()
                val future = result.documents.filter { it.getDate("fecha")?.after(now) == true }
                val first = future.mapNotNull { it.getDate("fecha") }.minOrNull() ?: evento.fecha
                val anchor = EventSchedule.shiftedSeriesStart(first, evento.fecha, date)
                val samePattern = weeks == evento.repeticiones && evento.diasSemana.toSet() == originalDays.toSet() &&
                    EventSchedule.atTime(evento.fecha, "00:00") == EventSchedule.atTime(date, "00:00")
                val dates = if (samePattern) {
                    future.mapNotNull { it.getDate("fecha")?.let { previous -> EventSchedule.atTime(previous, start) } }
                } else {
                    EventSchedule.occurrences(anchor, start, evento.diasSemana, weeks)
                }.filter { it.after(now) }
                if (dates.isEmpty() || future.size + dates.size > 500 ||
                    future.none { it.id == eventoId }
                ) {
                    setBusy(false)
                    Toast.makeText(this, getString(R.string.invalid_series, EventSchedule.MAX_WEEKS), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val byDay = future.associateBy { it.getDate("fecha")?.let { time -> EventSchedule.atTime(time, "00:00") } }
                val retained = mutableSetOf<String>()
                val batch = db.batch()
                for (occurrence in dates) {
                    val existing: DocumentSnapshot? = byDay[EventSchedule.atTime(occurrence, "00:00")]
                    val data = fields + mapOf("fecha" to occurrence, "diasSemana" to evento.diasSemana, "repeticiones" to weeks)
                    if (existing != null) {
                        retained.add(existing.id)
                        batch.update(existing.reference, data)
                    } else {
                        val ref = events.document()
                        batch.set(ref, evento.copy(eventId = ref.id, userId = uid, fecha = occurrence,
                            nombre = name, aula = fields["aula"] as String, horaInicio = start, horaFin = end,
                            color = fields["color"] as String, repeticiones = weeks))
                    }
                }
                future.filterNot { it.id in retained }.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener { finish() }.addOnFailureListener { showWriteError(it) }
            }
            .addOnFailureListener { showWriteError(it) }
    }

    private fun showWriteError(error: Exception) {
        setBusy(false)
        Log.w("EditarEvento", "Could not update event", error)
        Toast.makeText(this, R.string.error_updating_class_series, Toast.LENGTH_LONG).show()
    }

    private fun mostrarDialogoConfirmacionEliminar() {
        if (saving) return
        val uid = auth.currentUser?.uid ?: return
        val events = db.collection("users").document(uid).collection("events")
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_event_confirmation_title)
            .setMessage(getString(R.string.delete_event_confirmation_message2, evento.nombre))
            .setPositiveButton(if (editsSeries) R.string.delete_this_class_only else R.string.yes_delete) { _, _ ->
                setBusy(true)
                events.document(evento.eventId).delete()
                    .addOnSuccessListener {
                        ReminderStore(this).remove(uid, listOf(evento.eventId))
                        finish()
                    }.addOnFailureListener { showWriteError(it) }
            }
            .setNeutralButton(R.string.cancel_action, null)
        if (editsSeries) {
            dialog.setNegativeButton(R.string.delete_all_repeated_classes) { _, _ ->
                setBusy(true)
                events.whereEqualTo("seriesId", evento.seriesId).get().addOnSuccessListener { result ->
                    val batch = db.batch()
                    result.documents.forEach { batch.delete(it.reference) }
                    batch.commit().addOnSuccessListener {
                        ReminderStore(this).remove(uid, result.documents.map { it.id })
                        finish()
                    }.addOnFailureListener { showWriteError(it) }
                }.addOnFailureListener { showWriteError(it) }
            }
        }
        dialog.show()
    }
}
