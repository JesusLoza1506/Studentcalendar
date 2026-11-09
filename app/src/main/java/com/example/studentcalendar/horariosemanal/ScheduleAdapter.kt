package com.example.studentcalendar.horariosemanal

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentcalendar.R
import kotlin.math.roundToInt
import com.example.studentcalendar.vertareas.EventConstants

class ScheduleAdapter(
    private var dailyEventsMap: MutableMap<Int, MutableList<CalendarEvent>>,
    private val hourHeightPx: Int,
    private val onEventLongClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private var visibleDays: List<Int> = emptyList()

    private var filterStartTimeHour: Int = 0
    private var filterEndTimeHour: Int = 23

    constructor(
        dailyEventsMap: MutableMap<Int, MutableList<CalendarEvent>>,
        hourHeightPx: Int,
        onEventLongClick: (CalendarEvent) -> Unit,
        initialVisibleDays: List<Int>,
        initialStartTimeHour: Int,
        initialEndTimeHour: Int
    ) : this(dailyEventsMap, hourHeightPx, onEventLongClick) {
        this.visibleDays = initialVisibleDays
        this.filterStartTimeHour = initialStartTimeHour
        this.filterEndTimeHour = initialEndTimeHour
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dailyScheduleContainer: FrameLayout = itemView.findViewById(R.id.daily_schedule_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_schedule_column, parent, false)
        val density = parent.context.resources.displayMetrics.density
        val fixedColumnWidthPx = (120 * density).roundToInt()
        view.layoutParams = ViewGroup.LayoutParams(fixedColumnWidthPx, ViewGroup.LayoutParams.MATCH_PARENT)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val currentDay = visibleDays[position]
        val eventsForThisDay = dailyEventsMap[currentDay] ?: emptyList()
        holder.dailyScheduleContainer.removeAllViews()

        val numVisibleHours = filterEndTimeHour - filterStartTimeHour + 1
        val totalHeightOfVisibleHours = numVisibleHours * hourHeightPx
        holder.dailyScheduleContainer.layoutParams.height = totalHeightOfVisibleHours

        agregarLineasHorarias(holder.dailyScheduleContainer, filterStartTimeHour, filterEndTimeHour)

        for (event in eventsForThisDay) {
            val eventView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.item_event, holder.dailyScheduleContainer, false)

            val tvEventName = eventView.findViewById<TextView>(R.id.tvEventName)
            val tvEventTime = eventView.findViewById<TextView>(R.id.tvEventTime)
            val tvEventClassroom = eventView.findViewById<TextView>(R.id.tvEventClassroom)
            val eventBackground = eventView.findViewById<LinearLayout>(R.id.event_background)

            tvEventName.text = event.nombre
            tvEventClassroom.text = "📍 Aula: ${event.aula}"

            val eventColor = runCatching { Color.parseColor(event.color) }.getOrDefault(Color.DKGRAY)
            eventBackground.setBackgroundColor(eventColor)
            val textColor = if (esColorOscuro(eventColor)) Color.WHITE else Color.BLACK
            tvEventName.setTextColor(textColor)
            tvEventTime.setTextColor(textColor)
            tvEventClassroom.setTextColor(textColor)

            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            val startMinutes = convertirCadenaHoraAMinutos(event.horaInicio)
            val relativeTopMarginPx = (startMinutes - (filterStartTimeHour * 60)) / 60.0 * hourHeightPx
            layoutParams.topMargin = relativeTopMarginPx.roundToInt().coerceAtLeast(0)

            if (event.tipo == EventConstants.TYPE_CLASS && event.horaFin.isNotEmpty()) {
                val endMinutes = convertirCadenaHoraAMinutos(event.horaFin)
                val durationMinutes = minOf(endMinutes, (filterEndTimeHour + 1) * 60) - maxOf(startMinutes, filterStartTimeHour * 60)

                layoutParams.height = if (durationMinutes < 0) {
                    Log.w("ScheduleAdapter", "Duración negativa para evento ${event.nombre} (${event.horaInicio} - ${event.horaFin}). Asignando altura de 1 hora.")
                    hourHeightPx
                } else {
                    (durationMinutes / 60.0 * hourHeightPx).roundToInt()
                }
                tvEventTime.text = "${event.horaInicio} - ${event.horaFin}"
            } else {
                layoutParams.height = (0.5 * hourHeightPx).roundToInt()
                tvEventTime.text = event.horaInicio
            }

            eventView.layoutParams = layoutParams

            eventView.setOnLongClickListener {
                onEventLongClick(event)
                true
            }

            holder.dailyScheduleContainer.addView(eventView)
        }
    }

    override fun getItemCount(): Int {
        return visibleDays.size
    }

    fun actualizarDiasVisiblesYEventos(
        newVisibleDays: List<Int>,
        newDailyEventsMap: MutableMap<Int, MutableList<CalendarEvent>>,
        newStartTimeHour: Int,
        newEndTimeHour: Int
    ) {
        this.visibleDays = newVisibleDays
        this.dailyEventsMap = newDailyEventsMap
        this.filterStartTimeHour = newStartTimeHour
        this.filterEndTimeHour = newEndTimeHour
        notifyDataSetChanged()
    }

    private fun esColorOscuro(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun agregarLineasHorarias(container: FrameLayout, startHour: Int, endHour: Int) {
        for (i in startHour..endHour) {
            val line = View(container.context)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                1
            )
            layoutParams.topMargin = (i - startHour) * hourHeightPx
            line.layoutParams = layoutParams
            line.setBackgroundColor(container.context.getColor(R.color.kalendu_outline))
            container.addView(line)
        }
    }

    private fun convertirCadenaHoraAMinutos(timeString: String): Int {
        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                (hours * 60) + minutes
            } else {
                Log.e("TimeConverter", "Formato de hora incorrecto o nulo: '$timeString'")
                0
            }
        } catch (e: NumberFormatException) {
            Log.e("TimeConverter", "Error de formato de número al convertir hora: '$timeString'", e)
            0
        } catch (e: Exception) {
            Log.e("TimeConverter", "Error inesperado al convertir cadena de hora '$timeString' a minutos: ${e.message}", e)
            0
        }
    }
}
