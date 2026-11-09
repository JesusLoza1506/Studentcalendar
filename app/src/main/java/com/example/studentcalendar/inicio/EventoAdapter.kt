package com.example.studentcalendar.inicio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studentcalendar.databinding.ItemEventoBinding
import com.example.studentcalendar.horariosemanal.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*
import com.example.studentcalendar.vertareas.EventConstants
import com.example.studentcalendar.R

class CalendarEventAdapter(
    private val listaEventos: List<CalendarEvent>,
    private val onItemClick: (CalendarEvent) -> Unit,
    private val onDeleteClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<CalendarEventAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(val binding: ItemEventoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context = binding.root.context

        fun bind(evento: CalendarEvent) {
            binding.tvNombre.text = evento.nombre
            handleEventType(evento.tipo)
            handleClassroom(evento.aula)

            if (evento.tipo == EventConstants.TYPE_CLASS) {
                binding.tvHora.text = "${evento.horaInicio} - ${evento.horaFin}"
            } else {
                binding.tvHora.text = evento.horaInicio
            }

            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvFecha.text = formatoFecha.format(evento.fecha)

            handleRepetition(evento)
            handleColor(evento.color)

            binding.root.setOnClickListener {
                onItemClick(evento)
            }

            binding.tvDeleteIcon.setOnClickListener {
                onDeleteClick(evento)
            }
        }

        private fun handleEventType(eventType: String) {
            if (eventType == EventConstants.TYPE_CLASS) {
                binding.tvTipoEvento.text = context.getString(R.string.event_type_class_display)
                binding.tvIconoTipo.text = context.getString(R.string.event_icon_class)
                binding.tvTipoEvento.setBackgroundResource(R.drawable.bg_tipo_clase)
            } else if (eventType == EventConstants.TYPE_TASK) {
                binding.tvTipoEvento.text = context.getString(R.string.event_type_task_display)
                binding.tvIconoTipo.text = context.getString(R.string.event_icon_task)
                binding.tvTipoEvento.setBackgroundResource(R.drawable.bg_tipo_tarea)
            } else {
                binding.tvTipoEvento.text = "Evento"
                binding.tvIconoTipo.text = ""
                binding.tvTipoEvento.setBackgroundResource(R.drawable.bg_tipo_tarea)
            }
        }

        private fun handleClassroom(classroom: String) {
            if (classroom.isNotEmpty()) {
                binding.tvAula.text = context.getString(R.string.event_classroom_prefix, classroom)
                binding.tvAula.visibility = View.VISIBLE
            } else {
                binding.tvAula.visibility = View.GONE
            }
        }

        private fun handleRepetition(evento: CalendarEvent) {
            if (evento.tipo == EventConstants.TYPE_CLASS && evento.diasSemana.isNotEmpty()) {
                val semanas = evento.repeticiones
                binding.tvRepeticion.text = context.getString(R.string.event_repetition_display, semanas)
                binding.tvRepeticion.visibility = View.VISIBLE
            } else {
                binding.tvRepeticion.visibility = View.GONE
            }
        }


        private fun handleColor(colorHex: String) {
            try {
                val parsedColor = Color.parseColor(colorHex)
                binding.viewColor.setBackgroundColor(parsedColor) // Esto establece el color de la barra lateral

                val textColor = Color.BLACK // O Color.DKGRAY para un aspecto más suave

                binding.tvNombre.setTextColor(textColor)
                binding.tvHora.setTextColor(textColor)
                binding.tvFecha.setTextColor(textColor)
                binding.tvAula.setTextColor(textColor)
                binding.tvIconoTipo.setTextColor(textColor)
                binding.tvRepeticion.setTextColor(textColor)
                binding.tvDeleteIcon.setTextColor(textColor)
            } catch (e: Exception) {
                binding.viewColor.setBackgroundColor(Color.parseColor(context.getString(R.string.default_color_error)))
                val negro = Color.BLACK
                binding.tvNombre.setTextColor(negro)
                binding.tvHora.setTextColor(negro)
                binding.tvFecha.setTextColor(negro)
                binding.tvAula.setTextColor(negro)
                binding.tvIconoTipo.setTextColor(negro)
                binding.tvRepeticion.setTextColor(negro)
                binding.tvDeleteIcon.setTextColor(negro)
            }
        }

        private fun esColorOscuro(color: Int): Boolean {
            val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
            return darkness >= 0.4
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ItemEventoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        holder.bind(listaEventos[position])
    }

    override fun getItemCount(): Int = listaEventos.size
}
