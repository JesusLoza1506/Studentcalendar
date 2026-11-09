package com.example.studentcalendar.horariosemanal

import com.example.studentcalendar.notificaciones.ReminderStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.studentcalendar.eventos.EventSchedule
import com.example.studentcalendar.vertareas.EventConstants
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentcalendar.R
import com.example.studentcalendar.databinding.ActivityHorarioSemanalBinding
import com.example.studentcalendar.databinding.DialogFilterScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import com.example.studentcalendar.ajustes.LocaleHelper
import android.content.SharedPreferences
import com.example.studentcalendar.base.BaseActivity
import android.graphics.Color // Importar Color
import android.view.View

class HorarioSemanalActivity : BaseActivity<ActivityHorarioSemanalBinding>() {

    override fun getViewBinding(): ActivityHorarioSemanalBinding {
        return ActivityHorarioSemanalBinding.inflate(layoutInflater)
    }

    override fun getNavigationMenuItemId(): Int = R.id.nav_view_schedule

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentWeekCalendar: Calendar
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val dailyEventsMap = mutableMapOf<Int, MutableList<CalendarEvent>>()
    private var isRvScheduleScrolling = false
    private var isHeaderScrolling = false
    private var hourHeightPx: Int = 0
    private var filterStartTimeHour: Int = 0
    private var filterStartTimeMinute: Int = 0
    private var filterEndTimeHour: Int = 23
    private var filterEndTimeMinute: Int = 59
    private val PREFS_NAME = "HorarioSemanalPrefs"
    private val KEY_SHOW_MON_TO_FRI = "filterShowMonToFri"
    private val KEY_START_TIME_HOUR = "filterStartTimeHour"
    private val KEY_START_TIME_MINUTE = "filterStartTimeMinute"
    private val KEY_END_TIME_HOUR = "filterEndTimeHour"
    private val KEY_END_TIME_MINUTE = "filterEndTimeMinute"
    private var filterShowMonToFri: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        currentWeekCalendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val density = resources.displayMetrics.density
        hourHeightPx = (100 * density).roundToInt()

        cargarPreferenciasFiltro()
        configurarRecyclerViews()
        configurarListeners()
        actualizarVistaSemanal()
    }

    private fun cargarPreferenciasFiltro() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        filterShowMonToFri = prefs.getBoolean(KEY_SHOW_MON_TO_FRI, false)
        filterStartTimeHour = prefs.getInt(KEY_START_TIME_HOUR, 0)
        filterStartTimeMinute = prefs.getInt(KEY_START_TIME_MINUTE, 0)
        filterEndTimeHour = prefs.getInt(KEY_END_TIME_HOUR, 23)
        filterEndTimeMinute = prefs.getInt(KEY_END_TIME_MINUTE, 59)
    }

    private fun guardarPreferenciasFiltro() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(KEY_SHOW_MON_TO_FRI, filterShowMonToFri)
        editor.putInt(KEY_START_TIME_HOUR, filterStartTimeHour)
        editor.putInt(KEY_START_TIME_MINUTE, filterStartTimeMinute)
        editor.putInt(KEY_END_TIME_HOUR, filterEndTimeHour)
        editor.putInt(KEY_END_TIME_MINUTE, filterEndTimeMinute)
        editor.apply()
    }

    private fun poblarColumnaHorasFijas(startHour: Int, endHour: Int) {
        binding.hoursColumnLayout.removeAllViews()
        for (i in startHour..endHour) {
            val hourTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    hourHeightPx
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                text = String.format(Locale.getDefault(), "%02d:00", i)
                textSize = 10f
                setTextColor(getColor(R.color.kalendu_text))
                setPadding(8, 0, 8, 0)
            }
            binding.hoursColumnLayout.addView(hourTextView)
        }
    }

    private fun configurarRecyclerViews() {
        scheduleAdapter = ScheduleAdapter(
            dailyEventsMap,
            hourHeightPx,
            { event -> mostrarDialogoEliminarEvento(event) },
            obtenerDiasVisiblesEnOrden(currentWeekCalendar.clone() as Calendar, filterShowMonToFri),
            filterStartTimeHour,
            filterEndTimeHour
        )
        binding.rvSchedule.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSchedule.adapter = scheduleAdapter

        for (i in 0 until binding.rvSchedule.itemDecorationCount) {
            if (binding.rvSchedule.getItemDecorationAt(i) is DayDividerItemDecoration) {
                binding.rvSchedule.removeItemDecorationAt(i)
                break
            }
        }
        // Añadir el nuevo ItemDecoration
        val dividerWidthPx = (2 * resources.displayMetrics.density).roundToInt()
        val dividerColor = getColor(R.color.kalendu_outline)
        binding.rvSchedule.addItemDecoration(DayDividerItemDecoration(dividerWidthPx, dividerColor))


        binding.rvSchedule.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isHeaderScrolling) {
                    isRvScheduleScrolling = true
                    binding.horizontalScrollViewHeader.scrollTo(recyclerView.computeHorizontalScrollOffset(), 0)
                }
                isRvScheduleScrolling = false
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isRvScheduleScrolling = false
                }
            }
        })

        binding.horizontalScrollViewHeader.viewTreeObserver.addOnScrollChangedListener {
            if (!isRvScheduleScrolling) {
                isHeaderScrolling = true
                binding.rvSchedule.scrollBy(binding.horizontalScrollViewHeader.scrollX - binding.rvSchedule.computeHorizontalScrollOffset(), 0)
                isHeaderScrolling = false
            }
        }
    }

    private fun configurarListeners() {
        binding.btnPreviousWeek.setOnClickListener {
            currentWeekCalendar.add(Calendar.WEEK_OF_YEAR, -1)
            actualizarVistaSemanal()
        }

        binding.btnNextWeek.setOnClickListener {
            currentWeekCalendar.add(Calendar.WEEK_OF_YEAR, 1)
            actualizarVistaSemanal()
        }

        binding.fabFilter.setOnClickListener {
            mostrarDialogoFiltro()
        }
    }

    private fun actualizarVistaSemanal() {
        val startOfWeek = currentWeekCalendar.clone() as Calendar
        startOfWeek.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endOfWeek = startOfWeek.clone() as Calendar
        endOfWeek.add(Calendar.DATE, 6)
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23)
        endOfWeek.set(Calendar.MINUTE, 59)
        endOfWeek.set(Calendar.SECOND, 59)
        endOfWeek.set(Calendar.MILLISECOND, 999)

        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        binding.tvCurrentWeekRange.text = "${dateFormat.format(startOfWeek.time).uppercase(Locale.getDefault())} - ${dateFormat.format(endOfWeek.time).uppercase(Locale.getDefault())}"

        val numHourSlots = (filterEndTimeHour - filterStartTimeHour) + 1
        val totalHeightOfVisibleHours = numHourSlots * hourHeightPx


        binding.hoursColumnLayout.layoutParams = LinearLayout.LayoutParams(
            binding.hoursColumnLayout.layoutParams.width,
            totalHeightOfVisibleHours
        )

        poblarColumnaHorasFijas(filterStartTimeHour, filterEndTimeHour)
        actualizarEncabezadosDias(startOfWeek, filterShowMonToFri)

        cargarEventosParaSemana(startOfWeek, endOfWeek)

        binding.scrollViewGeneral.post {
            val initialScrollY = (filterStartTimeHour * hourHeightPx)
            val maxScroll = binding.hoursColumnLayout.height - binding.scrollViewGeneral.height
            val finalScrollY = Math.min(Math.max(0, initialScrollY), maxScroll)
            binding.scrollViewGeneral.scrollTo(0, finalScrollY)
        }
    }

    private fun actualizarEncabezadosDias(startOfWeek: Calendar, showMonToFri: Boolean) {
        binding.dayHeadersLayout.removeAllViews()

        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("dd", Locale.getDefault())

        val currentDay = startOfWeek.clone() as Calendar

        for (i in 0..6) {
            val dayCalendar = currentDay.clone() as Calendar
            dayCalendar.add(Calendar.DATE, i)

            val dayOfWeek = dayCalendar.get(Calendar.DAY_OF_WEEK)

            if (showMonToFri && (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) {
                continue
            }

            val dayHeaderView = LayoutInflater.from(this).inflate(R.layout.item_day_header, binding.dayHeadersLayout, false)
            val tvDayName = dayHeaderView.findViewById<TextView>(R.id.tvDayName)
            val tvDayDate = dayHeaderView.findViewById<TextView>(R.id.tvDayDate)

            tvDayName.text = dayOfWeekFormat.format(dayCalendar.time).uppercase(Locale.getDefault())
            tvDayDate.text = dayOfMonthFormat.format(dayCalendar.time)

            binding.dayHeadersLayout.addView(dayHeaderView)

            val tempVisibleDays = mutableListOf<Int>()
            for (j in 0..i) { // Recorrer hasta el día actual para contar los días ya añadidos
                val tempDayCalendar = startOfWeek.clone() as Calendar
                tempDayCalendar.add(Calendar.DATE, j)
                val tempDayOfWeek = tempDayCalendar.get(Calendar.DAY_OF_WEEK)
                if (!(showMonToFri && (tempDayOfWeek == Calendar.SATURDAY || tempDayOfWeek == Calendar.SUNDAY))) {
                    tempVisibleDays.add(tempDayOfWeek)
                }
            }

            if (tempVisibleDays.size < obtenerDiasVisiblesEnOrden(startOfWeek, showMonToFri).size) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (2 * resources.displayMetrics.density).roundToInt(), // Ancho de la línea (2dp)
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(getColor(R.color.kalendu_outline)) // Color de la línea
                }
                binding.dayHeadersLayout.addView(divider)
            }
        }
    }

    private var weekRequest = 0

    private fun cargarEventosParaSemana(startOfWeek: Calendar, endOfWeek: Calendar) {
        val userId = auth.currentUser?.uid ?: return
        val request = ++weekRequest

        db.collection("users").document(userId)
            .collection("events")
            .whereGreaterThanOrEqualTo("fecha", startOfWeek.time)
            .whereLessThanOrEqualTo("fecha", endOfWeek.time)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (request != weekRequest) return@addOnSuccessListener
                dailyEventsMap.clear()

                val filterStartMinutes = (filterStartTimeHour * 60) + filterStartTimeMinute
                val filterEndMinutes = (filterEndTimeHour * 60) + filterEndTimeMinute

                for (document in querySnapshot.documents) {
                    val event = document.toObject(CalendarEvent::class.java)
                    if (event != null) {
                        event.eventId = document.id

                        val eventDate = Calendar.getInstance().apply { time = event.fecha }
                        val dayOfWeek = eventDate.get(Calendar.DAY_OF_WEEK)

                        val shouldShowDay = if (filterShowMonToFri) {
                            dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY
                        } else {
                            true
                        }

                        if (!shouldShowDay) {
                            continue
                        }

                        val start = EventSchedule.minutes(event.horaInicio) ?: continue
                        val end = if (event.tipo == EventConstants.TYPE_TASK) null else EventSchedule.minutes(event.horaFin) ?: continue
                        val isWithinTimeRange = EventSchedule.visibleBetween(start, end, filterStartMinutes, filterEndMinutes)

                        if (isWithinTimeRange) {
                            dailyEventsMap.getOrPut(dayOfWeek) { mutableListOf() }.add(event)
                        }
                    }
                }

                val visibleDays = obtenerDiasVisiblesEnOrden(startOfWeek, filterShowMonToFri)
                scheduleAdapter.actualizarDiasVisiblesYEventos(
                    visibleDays,
                    dailyEventsMap,
                    filterStartTimeHour,
                    filterEndTimeHour
                )
            }
            .addOnFailureListener { exception ->
                if (request != weekRequest) return@addOnFailureListener
                Log.e("HorarioSemanalActivity", "Error al cargar eventos: ${exception.message}")
                Toast.makeText(this, "Error al cargar eventos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoEliminarEvento(event: CalendarEvent) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_event_title))
            .setMessage(getString(R.string.delete_event_message, event.nombre))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                eliminarEventoDeFirestore(event)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun eliminarEventoDeFirestore(event: CalendarEvent) {
        val userId = auth.currentUser?.uid ?: return
        if (event.eventId.isBlank()) return

        db.collection("users").document(userId)
            .collection("events")
            .document(event.eventId)
            .delete()
            .addOnSuccessListener {
                ReminderStore(this).remove(userId, listOf(event.eventId))
                actualizarVistaSemanal()
                Toast.makeText(this, "Evento eliminado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("HorarioSemanalActivity", "Error al eliminar evento: ${e.message}", e)
                Toast.makeText(this, "Error al eliminar evento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoFiltro() {
        val dialogBinding = DialogFilterScheduleBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root

        val dialogBuilder = MaterialAlertDialogBuilder(this)
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()

        alertDialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }

        dialogBinding.dialogTitle.text = getString(R.string.dialog_filter_schedule_title)
        dialogBinding.timeRangeLabel.text = getString(R.string.dialog_filter_time_range_label)
        dialogBinding.fromLabel.text = getString(R.string.dialog_filter_from_label)
        dialogBinding.toLabel.text = getString(R.string.dialog_filter_to_label)
        dialogBinding.showDaysLabel.text = getString(R.string.dialog_filter_show_days_label)
        dialogBinding.rbMonToFri.text = getString(R.string.dialog_filter_mon_to_fri)
        dialogBinding.rbMonToSun.text = getString(R.string.dialog_filter_mon_to_sun)
        dialogBinding.btnCancelFilter.text = getString(R.string.action_cancel)
        dialogBinding.btnApplyFilter.text = getString(R.string.action_apply)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val tempStartTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, filterStartTimeHour)
            set(Calendar.MINUTE, filterStartTimeMinute)
        }
        val tempEndTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, filterEndTimeHour)
            set(Calendar.MINUTE, filterEndTimeMinute)
        }

        dialogBinding.btnStartTime.text = timeFormat.format(tempStartTime.time)
        dialogBinding.btnEndTime.text = timeFormat.format(tempEndTime.time)

        if (filterShowMonToFri) {
            dialogBinding.rbMonToFri.isChecked = true
        } else {
            dialogBinding.rbMonToSun.isChecked = true
        }

        dialogBinding.btnCancelFilter.setOnClickListener {
            alertDialog.dismiss()
        }

        dialogBinding.btnStartTime.setOnClickListener {
            val currentHour = tempStartTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = tempStartTime.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                tempStartTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                tempStartTime.set(Calendar.MINUTE, minute)
                dialogBinding.btnStartTime.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            }, currentHour, currentMinute, true)
            timePickerDialog.show()
        }

        dialogBinding.btnEndTime.setOnClickListener {
            val currentHour = tempEndTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = tempEndTime.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                tempEndTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                tempEndTime.set(Calendar.MINUTE, minute)
                dialogBinding.btnEndTime.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            }, currentHour, currentMinute, true)
            timePickerDialog.show()
        }

        dialogBinding.btnApplyFilter.setOnClickListener {
            val from = tempStartTime.get(Calendar.HOUR_OF_DAY) * 60 + tempStartTime.get(Calendar.MINUTE)
            val to = tempEndTime.get(Calendar.HOUR_OF_DAY) * 60 + tempEndTime.get(Calendar.MINUTE)
            if (from >= to) {
                Toast.makeText(this, R.string.toast_start_time_after_end_time, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            filterStartTimeHour = tempStartTime.get(Calendar.HOUR_OF_DAY)
            filterStartTimeMinute = tempStartTime.get(Calendar.MINUTE)
            filterEndTimeHour = tempEndTime.get(Calendar.HOUR_OF_DAY)
            filterEndTimeMinute = tempEndTime.get(Calendar.MINUTE)

            filterShowMonToFri = (dialogBinding.rgDayRange.checkedRadioButtonId == R.id.rbMonToFri)

            if (filterStartTimeHour * 60 + filterStartTimeMinute >= filterEndTimeHour * 60 + filterEndTimeMinute) {
                Toast.makeText(this, getString(R.string.toast_start_time_after_end_time), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            guardarPreferenciasFiltro()

            alertDialog.dismiss()
            aplicarFiltrosHorario()
        }

        alertDialog.show()
    }

    private fun obtenerDiasVisiblesEnOrden(startOfWeek: Calendar, showMonToFri: Boolean): List<Int> {
        val visibleDays = mutableListOf<Int>()
        val currentDay = startOfWeek.clone() as Calendar

        for (i in 0..6) {
            val dayCalendar = currentDay.clone() as Calendar
            dayCalendar.add(Calendar.DATE, i)
            val dayOfWeek = dayCalendar.get(Calendar.DAY_OF_WEEK)

            if (showMonToFri) {
                if (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY) {
                    visibleDays.add(dayOfWeek)
                }
            } else {
                visibleDays.add(dayOfWeek)
            }
        }
        return visibleDays
    }

    private fun aplicarFiltrosHorario() {
        actualizarVistaSemanal()
        Toast.makeText(this, getString(R.string.toast_filters_applied), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        cargarPreferenciasFiltro()
        actualizarVistaSemanal()
    }
}
