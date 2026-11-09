package com.example.studentcalendar

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.databinding.ActivityAnadirClaseTareaBinding
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.example.studentcalendar.notificaciones.NotificationHelper
import com.example.studentcalendar.eventos.EventSchedule
import android.util.Log
import android.view.ViewGroup
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.ajustes.ThemeHelper
import com.example.studentcalendar.vertareas.EventConstants
import android.net.Uri
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.os.VibrationEffect
import android.os.Vibrator
import java.util.LinkedHashMap
class AnadirClaseTareaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnadirClaseTareaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedStartTime: Calendar? = null
    private var selectedEndTime: Calendar? = null
    private var selectedEventDate: Calendar? = null
    private var selectedRepetitionStartDate: Calendar? = null
    private var selectedColor: Int = -1
    private var notificacionActivada: Boolean = false
    private var minutosAntes: Int = 10
    private lateinit var llSoundOptionsContainer: LinearLayout
    private lateinit var swAlarmSound: Switch
    private lateinit var llSoundTypeSelection: LinearLayout
    private lateinit var rgSoundSource: RadioGroup
    private lateinit var rbAppSound: RadioButton
    private lateinit var rbCustomSound: RadioButton
    private lateinit var spinnerAppSounds: Spinner
    private lateinit var llCustomSoundSelection: LinearLayout
    private lateinit var btnSelectCustomSound: Button
    private lateinit var tvSelectedCustomSound: TextView
    private val appSoundNames = mutableListOf<String>()
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingSoundUri: Uri? = null
    private lateinit var btnPreviewSound: Button
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { originalUri ->
            copyUriToFile(originalUri)?.let { copiedUri ->
                tvSelectedCustomSound.text = getFileName(originalUri) ?: getString(R.string.no_sound_selected_label)
                selectedCustomSoundUri = copiedUri
                btnPreviewSound.visibility = View.VISIBLE
                Toast.makeText(this, getString(R.string.sound_selected_and_copied), Toast.LENGTH_SHORT).show()
            } ?: run {
                tvSelectedCustomSound.text = getString(R.string.no_sound_selected_label)
                selectedCustomSoundUri = null
                btnPreviewSound.visibility = View.GONE
                Toast.makeText(this, getString(R.string.sound_selection_error), Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            tvSelectedCustomSound.text = getString(R.string.no_sound_selected_label)
            selectedCustomSoundUri = null
            btnPreviewSound.visibility = View.GONE
            Toast.makeText(this, getString(R.string.sound_selection_error), Toast.LENGTH_SHORT).show()
        }
    }
    private var selectedCustomSoundUri: Uri? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickAudioLauncher.launch("audio/*")
        } else {
            Toast.makeText(this, getString(R.string.sound_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }
    // -----------------------------------------------------------
    // --- NUEVAS DECLARACIONES PARA LA FUNCIONALIDAD DE VIBRACIÓN ---
    private lateinit var llVibrationOptionsContainer: LinearLayout
    private lateinit var swVibration: Switch
    private lateinit var spinnerVibrationPattern: Spinner
    private lateinit var btnTestVibration: Button
    // Map para almacenar los patrones de vibración (nombre visible -> patrón de milisegundos)
    private lateinit var vibrationPatterns: LinkedHashMap<String, LongArray?> // longArrayOf o null para "Ninguna"
    private var currentSelectedVibrationPattern: LongArray? = null // Para almacenar el patrón seleccionado
    // ---------------------------------------------------------------
    private val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    private val predefinedColors = mapOf(
        R.string.color_light_blue to Color.parseColor("#3498DB"),
        R.string.color_red to Color.parseColor("#E74C3C"),
        R.string.color_green to Color.parseColor("#2ECC71"),
        R.string.color_purple to Color.parseColor("#9B59B6"),
        R.string.color_yellow to Color.parseColor("#F1C40F"),
        R.string.color_turquoise to Color.parseColor("#1ABC9C"),
        R.string.color_dark_orange to Color.parseColor("#E67E22"),
        R.string.color_dark_blue to Color.parseColor("#34495E"),
        R.string.color_silver to Color.parseColor("#95A5A6"),
        R.string.color_dark_purple to Color.parseColor("#7D3C98"),
        R.string.color_pink to Color.parseColor("#FF69B4"),
        R.string.color_blue_violet to Color.parseColor("#8A2BE2"),
        R.string.color_sea_green to Color.parseColor("#20B2AA"),
        R.string.color_gold to Color.parseColor("#FFD700"),
        R.string.color_slate_blue to Color.parseColor("#6A5ACD")
    )
    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(context)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.aplicarTema(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAnadirClaseTareaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        binding.AddEvent.text = getString(R.string.add_event_title)
        binding.etEventName.hint = getString(R.string.hint_event_name)
        binding.etClassroom.hint = getString(R.string.hint_classroom)
        binding.tvEventTypeLabel.text = getString(R.string.label_event_type)
        binding.rbClass.text = getString(R.string.radio_button_class)
        binding.rbTask.text = getString(R.string.radio_button_task)
        binding.tvDaysLabel.text = getString(R.string.label_days_for_classes)
        binding.cbMonday.text = getString(R.string.day_monday_short)
        binding.cbTuesday.text = getString(R.string.day_tuesday_short)
        binding.cbWednesday.text = getString(R.string.day_wednesday_short)
        binding.cbThursday.text = getString(R.string.day_thursday_short)
        binding.cbFriday.text = getString(R.string.day_friday_short)
        binding.cbSaturday.text = getString(R.string.day_saturday_short)
        binding.cbSunday.text = getString(R.string.day_sunday_short)
        binding.tvRepeatLabel.text = getString(R.string.label_repeat_by)
        binding.etRepeatWeeks.hint = getString(R.string.hint_repeat_weeks_default)
        binding.tvWeeksLabel.text = getString(R.string.label_weeks)
        binding.btnSelectRepetitionStartDate.text = getString(R.string.start_repetition_date_button)
        binding.btnSelectStartTime.text = getString(R.string.start_time_button)
        binding.btnSelectEndTime.text = getString(R.string.end_time_button)
        binding.btnSelectDate.text = getString(R.string.select_date_button)
        binding.tvNotificacion.text = getString(R.string.activate_reminder_switch)
        binding.btnSelectColor.text = getString(R.string.color_button)
        binding.btnSaveEvent.text = getString(R.string.save_event_button)
        // --- INICIALIZACIÓN DE VISTAS DE SONIDO (EXISTENTES) ---
        llSoundOptionsContainer = binding.llSoundOptionsContainer
        swAlarmSound = binding.swAlarmSound
        llSoundTypeSelection = binding.llSoundTypeSelection
        rgSoundSource = binding.rgSoundSource
        rbAppSound = binding.rbAppSound
        rbCustomSound = binding.rbCustomSound
        spinnerAppSounds = binding.spinnerAppSounds
        llCustomSoundSelection = binding.llCustomSoundSelection
        btnSelectCustomSound = binding.btnSelectCustomSound
        tvSelectedCustomSound = binding.tvSelectedCustomSound
        btnPreviewSound = binding.btnPreviewSound
        // ----------------------------------------------------------
        // --- INICIALIZACIÓN DE NUEVAS VISTAS DE VIBRACIÓN ---
        llVibrationOptionsContainer = binding.llVibrationOptionsContainer
        swVibration = binding.swVibration
        spinnerVibrationPattern = binding.spinnerVibrationPattern
        btnTestVibration = binding.btnTestVibration
        // ---------------------------------------------------
        configurarListeners()
        configurarEstadoInicial()
        setupAppSoundSpinner() // Llamada para configurar el spinner de sonidos de la app
        setupVibrationPatternsSpinner() // ¡Nueva llamada para configurar el spinner de vibración!
    }
    override fun onDestroy() {
        super.onDestroy()
        stopSoundPreview()
    }
    private fun configurarListeners() {
        binding.rgEventType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_class -> {
                    binding.tvDaysLabel.visibility = View.VISIBLE
                    binding.llDaysCheckboxes.visibility = View.VISIBLE
                    binding.layoutRepeatWeeks.visibility = View.VISIBLE
                    binding.etClassroom.visibility = View.VISIBLE
                    binding.etClassroom.hint = getString(R.string.hint_classroom)
                    binding.btnSelectEndTime.visibility = View.VISIBLE
                    binding.btnSelectDate.visibility = View.GONE
                    binding.btnSelectRepetitionStartDate.visibility = View.VISIBLE
                    selectedEventDate = null
                    binding.llNotificationOptions.visibility = View.VISIBLE
                }
                R.id.rb_task -> {
                    binding.tvDaysLabel.visibility = View.GONE
                    binding.llDaysCheckboxes.visibility = View.GONE
                    binding.layoutRepeatWeeks.visibility = View.GONE
                    binding.etClassroom.hint = getString(R.string.hint_classroom_optional)
                    binding.etClassroom.visibility = View.VISIBLE
                    binding.btnSelectEndTime.visibility = View.GONE
                    binding.btnSelectDate.visibility = View.VISIBLE
                    binding.btnSelectRepetitionStartDate.visibility = View.GONE
                    if (selectedEventDate == null) {
                        selectedEventDate = Calendar.getInstance()
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        binding.btnSelectDate.text = "${getString(R.string.date_prefix)} ${dateFormat.format(selectedEventDate!!.time)}"
                    }
                    binding.llNotificationOptions.visibility = View.VISIBLE
                }
            }
        }
        binding.btnSelectStartTime.setOnClickListener { mostrarDialogoSelectorHora(true) }
        binding.btnSelectEndTime.setOnClickListener { mostrarDialogoSelectorHora(false) }
        binding.btnSelectDate.setOnClickListener { mostrarDialogoSelectorFecha() }
        binding.btnSelectRepetitionStartDate.setOnClickListener { mostrarDialogoSelectorFechaRepeticion() }
        binding.btnSelectColor.setOnClickListener { mostrarDialogoSelectorColor() }
        binding.btnSaveEvent.setOnClickListener { guardarEvento() }
        binding.swNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
                    } else {
                        notificacionActivada = true
                        binding.spinnerReminderTime.visibility = View.VISIBLE
                        llSoundOptionsContainer.visibility = View.VISIBLE // Mostrar contenedor de opciones de sonido
                        llVibrationOptionsContainer.visibility = View.VISIBLE // ¡Mostrar contenedor de opciones de vibración!
                        Toast.makeText(this, getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    notificacionActivada = true
                    binding.spinnerReminderTime.visibility = View.VISIBLE
                    llSoundOptionsContainer.visibility = View.VISIBLE // Mostrar contenedor de opciones de sonido
                    llVibrationOptionsContainer.visibility = View.VISIBLE // ¡Mostrar contenedor de opciones de vibración!
                    Toast.makeText(this, getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show()
                }
            } else {
                notificacionActivada = false
                binding.spinnerReminderTime.visibility = View.GONE
                minutosAntes = 10
                llSoundOptionsContainer.visibility = View.GONE // Ocultar contenedor de opciones de sonido
                swAlarmSound.isChecked = false // Desactivar también el sonido de alarma
                llVibrationOptionsContainer.visibility = View.GONE // ¡Ocultar contenedor de opciones de vibración!
                swVibration.isChecked = false // Desactivar la vibración
                Toast.makeText(this, getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show()
            }
        }
        binding.spinnerReminderTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val reminderOptions = resources.getStringArray(R.array.reminder_options)
                val selectedOption = reminderOptions[position]
                minutosAntes = when (selectedOption) {
                    getString(R.string.reminder_5_minutes_option) -> 5
                    getString(R.string.reminder_10_minutes_option) -> 10
                    getString(R.string.reminder_15_minutes_option) -> 15
                    getString(R.string.reminder_30_minutes_option) -> 30
                    getString(R.string.reminder_60_minutes_option) -> 60
                    else -> 10
                }
                Toast.makeText(this@AnadirClaseTareaActivity, getString(R.string.notification_x_minutes_before, minutosAntes), Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
        // --- LISTENERS PARA LA FUNCIONALIDAD DE SONIDO (EXISTENTES) ---
        swAlarmSound.setOnCheckedChangeListener { _, isChecked ->
            llSoundTypeSelection.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                stopSoundPreview()
            } else {
                updateSoundSelectionUI()
            }
        }
        rgSoundSource.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_app_sound -> {
                    spinnerAppSounds.visibility = View.VISIBLE
                    llCustomSoundSelection.visibility = View.GONE
                    tvSelectedCustomSound.text = getString(R.string.no_sound_selected_label)
                    selectedCustomSoundUri = null
                }
                R.id.rb_custom_sound -> {
                    spinnerAppSounds.visibility = View.GONE
                    llCustomSoundSelection.visibility = View.VISIBLE
                }
            }
            stopSoundPreview()
            updateSoundSelectionUI()
        }
        btnSelectCustomSound.setOnClickListener {
            checkAndRequestAudioPermission()
        }
        btnPreviewSound.setOnClickListener {
            playSoundPreview()
        }
        spinnerAppSounds.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    btnPreviewSound.visibility = View.VISIBLE
                } else {
                    btnPreviewSound.visibility = View.GONE
                }
                stopSoundPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                btnPreviewSound.visibility = View.GONE
                stopSoundPreview()
            }
        }
        // -----------------------------------------------------------
        // --- NUEVOS LISTENERS PARA LA FUNCIONALIDAD DE VIBRACIÓN ---
        swVibration.setOnCheckedChangeListener { _, isChecked ->
            spinnerVibrationPattern.visibility = if (isChecked) View.VISIBLE else View.GONE
            btnTestVibration.visibility = if (isChecked) View.VISIBLE else View.GONE
            // Si la vibración se desactiva, asegurar que el patrón seleccionado sea "Ninguna"
            if (!isChecked) {
                spinnerVibrationPattern.setSelection(0) // Asume que "Ninguna" es la primera opción
            }
        }
        spinnerVibrationPattern.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPatternName = parent?.getItemAtPosition(position).toString()
                currentSelectedVibrationPattern = vibrationPatterns[selectedPatternName]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentSelectedVibrationPattern = vibrationPatterns["Ninguna"] // O establecer un patrón predeterminado
            }
        }
        btnTestVibration.setOnClickListener {
            testVibration(currentSelectedVibrationPattern)
        }
        // -----------------------------------------------------------
    }
    private fun configurarEstadoInicial() {
        binding.rbClass.isChecked = true
        binding.tvDaysLabel.visibility = View.VISIBLE
        binding.llDaysCheckboxes.visibility = View.VISIBLE
        binding.layoutRepeatWeeks.visibility = View.VISIBLE
        binding.etClassroom.visibility = View.VISIBLE
        binding.etClassroom.hint = getString(R.string.hint_classroom)
        binding.btnSelectEndTime.visibility = View.VISIBLE
        binding.btnSelectDate.visibility = View.GONE
        binding.btnSelectRepetitionStartDate.visibility = View.VISIBLE
        selectedRepetitionStartDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.btnSelectRepetitionStartDate.text = "${getString(R.string.start_repetition_prefix)} ${dateFormat.format(selectedRepetitionStartDate!!.time)}"
        if (selectedColor == -1) {
            selectedColor = predefinedColors[R.string.color_light_blue] ?: Color.parseColor("#3498DB")
            actualizarFondoBotonColor()
        }
        val opcionesSpinner = resources.getStringArray(R.array.reminder_options)
        val adapter = ArrayAdapter(this, R.layout.spinner_item_dynamic_text, opcionesSpinner)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dynamic_text)
        binding.spinnerReminderTime.adapter = adapter
        binding.spinnerReminderTime.setSelection(opcionesSpinner.indexOf(getString(R.string.reminder_10_minutes_option)))
        // --- VISIBILIDAD INICIAL DE LAS OPCIONES DE NOTIFICACIÓN, SONIDO Y VIBRACIÓN ---
        binding.spinnerReminderTime.visibility = if (binding.swNotificaciones.isChecked) View.VISIBLE else View.GONE
        notificacionActivada = binding.swNotificaciones.isChecked
        // Visibilidad inicial del contenedor de opciones de sonido y vibración
        llSoundOptionsContainer.visibility = if (binding.swNotificaciones.isChecked) View.VISIBLE else View.GONE
        llVibrationOptionsContainer.visibility = if (binding.swNotificaciones.isChecked) View.VISIBLE else View.GONE // ¡Nuevo!
        // Visibilidad inicial de la selección de tipo de sonido (depende de swAlarmSound)
        llSoundTypeSelection.visibility = if (swAlarmSound.isChecked) View.VISIBLE else View.GONE
        // Configurar la UI inicial del sonido (qué RadioButton está marcado y su contenido)
        rgSoundSource.check(R.id.rb_app_sound)
        updateSoundSelectionUI()
        // Configurar visibilidad inicial de las opciones de vibración (depende de swVibration)
        spinnerVibrationPattern.visibility = if (swVibration.isChecked) View.VISIBLE else View.GONE
        btnTestVibration.visibility = if (swVibration.isChecked) View.VISIBLE else View.GONE
        // Asegurar que el spinner de vibración muestre "Ninguna" por defecto al inicio
        spinnerVibrationPattern.setSelection(0)
        // --------------------------------------------------------------------------------
    }
    private fun setupAppSoundSpinner() {
        // soundResIds se mantiene igual para obtener los IDs de los recursos
        val soundResIds = (1..10).map { resources.getIdentifier("sonido$it", "raw", packageName) }
        appSoundNames.clear()
        appSoundNames.add(getString(R.string.default_app_sound_name))
        // Añadir los nombres desde strings.xml
        for (i in 1..10) {
            val stringResId = resources.getIdentifier("sound_${i}_name", "string", packageName)
            if (stringResId != 0) {
                appSoundNames.add(getString(stringResId))
            } else {
                appSoundNames.add("Sonido $i") // Fallback por si no encuentra la cadena
            }
        }
        val adapter = ArrayAdapter(this, R.layout.spinner_item_dynamic_text, appSoundNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dynamic_text)
        spinnerAppSounds.adapter = adapter
    }
    private fun updateSoundSelectionUI() {
        if (rgSoundSource.checkedRadioButtonId == R.id.rb_app_sound) {
            spinnerAppSounds.visibility = View.VISIBLE
            llCustomSoundSelection.visibility = View.GONE
            btnPreviewSound.visibility = if (spinnerAppSounds.selectedItemPosition > 0) View.VISIBLE else View.GONE
        } else {
            spinnerAppSounds.visibility = View.GONE
            llCustomSoundSelection.visibility = View.VISIBLE
            btnPreviewSound.visibility = if (selectedCustomSoundUri != null) View.VISIBLE else View.GONE
        }
    }
    private fun checkAndRequestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pickAudioLauncher.launch("audio/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickAudioLauncher.launch("audio/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }
    private fun copyUriToFile(uri: Uri): Uri? {
        val fileName = getFileName(uri) ?: "custom_sound_${System.currentTimeMillis()}.mp3"
        val extension = fileName.substringAfterLast('.', "audio").takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) } ?: "audio"
        val destFile = File(filesDir, "custom_sounds/${UUID.randomUUID()}.$extension")
        destFile.parentFile?.mkdirs()
        try {
            val input = contentResolver.openInputStream(uri) ?: return null
            input.use { inputStream -> FileOutputStream(destFile).use { inputStream.copyTo(it) } }
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e("AnadirClaseTarea", "Error al copiar archivo de sonido: ${e.message}", e)
            Toast.makeText(this, getString(R.string.error_copying_sound), Toast.LENGTH_SHORT).show()
            return null
        }
    }
    private fun playSoundPreview() {
        stopSoundPreview()
        val soundUri: Uri? = if (rgSoundSource.checkedRadioButtonId == R.id.rb_app_sound) {
            val selectedPosition = spinnerAppSounds.selectedItemPosition
            if (selectedPosition > 0) { // 'selectedPosition' 0 es "Sonido Predeterminado"
                // Construye el nombre del recurso directamente como "sonidoX"
                // Asumiendo que tus archivos raw se llaman sonido1, sonido2, etc.
                val resourceName = "sonido$selectedPosition"
                val resId = resources.getIdentifier(resourceName, "raw", packageName)
                if (resId != 0) Uri.parse("android.resource://$packageName/$resId") else null
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } else {
            selectedCustomSoundUri
        }

        soundUri?.let { uri ->
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AnadirClaseTareaActivity, uri)
                    prepareAsync()
                    setOnPreparedListener { player ->
                        player.start()
                        currentPlayingSoundUri = uri

                        player.setOnCompletionListener {
                            stopSoundPreview()
                        }

                        // Detener la previsualización después de 5 segundos para evitar que siga sonando indefinidamente
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (currentPlayingSoundUri == uri && mediaPlayer != null && mediaPlayer!!.isPlaying) {
                                stopSoundPreview()
                            }
                        }, 5000) // 5 segundos
                    }
                    setOnErrorListener { _, _, _ ->
                        Toast.makeText(this@AnadirClaseTareaActivity, getString(R.string.sound_preview_error), Toast.LENGTH_SHORT).show()
                        stopSoundPreview()
                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("AnadirClaseTarea", "Error al iniciar previsualización: ${e.message}")
                Toast.makeText(this, getString(R.string.sound_preview_error), Toast.LENGTH_SHORT).show()
                stopSoundPreview()
            }
        } ?: Toast.makeText(this, getString(R.string.sound_not_selected_toast), Toast.LENGTH_SHORT).show()
    }

    private fun stopSoundPreview() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        currentPlayingSoundUri = null
    }
    // -----------------------------------------------------------

    // --- NUEVAS FUNCIONES PARA LA FUNCIONALIDAD DE VIBRACIÓN ---
    private fun setupVibrationPatternsSpinner() {
        vibrationPatterns = LinkedHashMap()
        // Usar las cadenas de recursos para los nombres de los patrones
        vibrationPatterns[getString(R.string.vibration_pattern_none)] = null // Sin vibración
        vibrationPatterns[getString(R.string.vibration_pattern_short)] = longArrayOf(0, 200) // Vibra por 200ms
        vibrationPatterns[getString(R.string.vibration_pattern_long)] = longArrayOf(0, 500) // Vibra por 500ms
        vibrationPatterns[getString(R.string.vibration_pattern_double_short)] = longArrayOf(0, 150, 100, 150) // Vibra 150ms, pausa 100ms, vibra 150ms
        vibrationPatterns[getString(R.string.vibration_pattern_heartbeat)] = longArrayOf(0, 100, 50, 100, 200, 100, 50, 100) // Vibra, pausa, vibra, pausa larga, vibra, pausa, vibra

        val adapter = ArrayAdapter(this, R.layout.spinner_item_dynamic_text, vibrationPatterns.keys.toTypedArray())
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dynamic_text)
        spinnerVibrationPattern.adapter = adapter
    }

    private fun testVibration(pattern: LongArray?) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (vibrator != null && vibrator.hasVibrator()) {
            if (pattern != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 para no repetir
                } else {
                    @Suppress("DEPRECATION") // Para versiones anteriores
                    vibrator.vibrate(pattern, -1) // -1 para no repetir
                }
            } else {
                // Usar la cadena de recurso para el toast
                Toast.makeText(this, getString(R.string.vibration_disabled_toast), Toast.LENGTH_SHORT).show()
            }
        } else {
            // Usar la cadena de recurso para el toast
            Toast.makeText(this, getString(R.string.device_no_vibration_support_toast), Toast.LENGTH_SHORT).show()
        }
    }
    // -----------------------------------------------------------

    private fun mostrarDialogoSelectorHora(esHoraInicio: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val formattedTime = SimpleDateFormat("HH:mm", Locale.ROOT).format(selectedCalendar.time)
            if (esHoraInicio) {
                selectedStartTime = selectedCalendar
                binding.btnSelectStartTime.text = "${getString(R.string.start_time_prefix)} $formattedTime"
            } else {
                selectedEndTime = selectedCalendar
                binding.btnSelectEndTime.text = "${getString(R.string.end_time_prefix)} $formattedTime"
            }
        }, hour, minute, true).show()
    }

    private fun mostrarDialogoSelectorFecha() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedEventDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.btnSelectDate.text = "${getString(R.string.date_prefix)} ${dateFormat.format(selectedEventDate!!.time)}"
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun mostrarDialogoSelectorFechaRepeticion() {
        val calendar = selectedRepetitionStartDate ?: Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedRepetitionStartDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.btnSelectRepetitionStartDate.text = "${getString(R.string.start_repetition_prefix)} ${dateFormat.format(selectedRepetitionStartDate!!.time)}"
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun mostrarDialogoSelectorColor() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getString(R.string.select_color_title))

        val layout = layoutInflater.inflate(R.layout.dialog_color_selector, null)
        val contenedor = layout.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.colorGrid)

        contenedor.removeAllViews()

        predefinedColors.forEach { (stringResId, colorValue) ->
            val colorView = View(this).apply {
                setBackgroundColor(colorValue)
                val tamaño = resources.getDimensionPixelSize(R.dimen.color_box_size)
                val margin = resources.getDimensionPixelSize(R.dimen.color_box_margin)
                layoutParams = ViewGroup.MarginLayoutParams(tamaño, tamaño).apply {
                    setMargins(margin, margin, margin, margin)
                }
            }
            contenedor.addView(colorView)
        }

        val dialog = builder.setView(layout)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        for (i in 0 until contenedor.childCount) {
            val colorView = contenedor.getChildAt(i)
            val colorEntry = predefinedColors.entries.elementAt(i)
            colorView.setOnClickListener {
                selectedColor = colorEntry.value
                actualizarFondoBotonColor()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun actualizarFondoBotonColor() {
        binding.btnSelectColor.setBackgroundColor(selectedColor)
        val textColor = if (esColorOscuro(selectedColor)) Color.WHITE else Color.BLACK
        binding.btnSelectColor.setTextColor(textColor)
    }

    private fun esColorOscuro(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun guardarEvento() {
        val startTime = SimpleDateFormat("HH:mm", Locale.ROOT)

        val eventName = binding.etEventName.text.toString().trim()
        val classroom = binding.etClassroom.text.toString().trim()
        val isClass = binding.rbClass.isChecked

        if (eventName.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_complete_required_fields), Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedColor == -1) {
            Toast.makeText(this, getString(R.string.toast_select_color), Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedStartTime == null) {
            Toast.makeText(this, getString(R.string.toast_select_start_time), Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.toast_user_not_logged_in), Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid
        val eventsCollectionRef = db.collection("users").document(userId).collection("events")

        // --- PREPARACIÓN DE DATOS DE SONIDO PARA CALENDAREVENT ---
        var sonidoAlarmaActivado = false
        var uriSonidoAlarma: String? = null
        var esSonidoPersonalizado = false
        var duracionSonidoPersonalizado: Long? = null

        if (binding.swNotificaciones.isChecked && swAlarmSound.isChecked) {
            sonidoAlarmaActivado = true
            if (rgSoundSource.checkedRadioButtonId == R.id.rb_app_sound) {
                val selectedPosition = spinnerAppSounds.selectedItemPosition
                if (selectedPosition > 0) {
                    // ¡AQUÍ ESTÁ EL CAMBIO! Construye el nombre del recurso directamente
                    // como "sonidoX" para que coincida con tus archivos raw.
                    val resourceName = "sonido$selectedPosition"
                    val resId = resources.getIdentifier(resourceName, "raw", packageName)
                    if (resId != 0) {
                        uriSonidoAlarma = "android.resource://$packageName/$resId"
                        esSonidoPersonalizado = false
                        duracionSonidoPersonalizado = null
                    } else {
                        sonidoAlarmaActivado = false
                        uriSonidoAlarma = null
                        esSonidoPersonalizado = false
                        duracionSonidoPersonalizado = null
                        Toast.makeText(this, getString(R.string.sound_selection_error) + ". " + getString(R.string.alarm_sound_title) + ": " + getString(R.string.default_app_sound_name) + ".", Toast.LENGTH_LONG).show()
                    }
                } else {
                    uriSonidoAlarma = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                    esSonidoPersonalizado = false
                    duracionSonidoPersonalizado = null
                }
            } else { // Sonido Personalizado
                if (selectedCustomSoundUri != null) {
                    uriSonidoAlarma = selectedCustomSoundUri.toString()
                    esSonidoPersonalizado = true
                    duracionSonidoPersonalizado = 10000L // Duración predeterminada para sonidos personalizados
                } else {
                    sonidoAlarmaActivado = false
                    uriSonidoAlarma = null
                    esSonidoPersonalizado = false
                    duracionSonidoPersonalizado = null
                    Toast.makeText(this, getString(R.string.sound_not_selected_toast) + " " + getString(R.string.alarm_sound_title) + ": " + getString(R.string.default_app_sound_name) + ".", Toast.LENGTH_LONG).show()
                }
            }
        } else { // Notificaciones o sonido de alarma desactivados
            sonidoAlarmaActivado = false
            uriSonidoAlarma = null
            esSonidoPersonalizado = false
            duracionSonidoPersonalizado = null
        }
        // ---------------------------------------------------------

        // --- PREPARACIÓN DE DATOS DE VIBRACIÓN PARA CALENDAREVENT (NUEVO) ---
        var vibracionActivada = false
        var patronVibracion: String? = null // Guardaremos el nombre del patrón como String

        if (binding.swNotificaciones.isChecked && swVibration.isChecked) {
            vibracionActivada = true
            patronVibracion = listOf(null, "short", "long", "double_short", "heartbeat")
                .getOrNull(spinnerVibrationPattern.selectedItemPosition)
            vibracionActivada = patronVibracion != null
        }

        // LÓGICA PARA CLASES
        if (isClass) {
            val selectedDaysInt = obtenerDiasSeleccionadosComoInt()
            val repeatWeeks = binding.etRepeatWeeks.text.toString().ifBlank { "1" }.toIntOrNull() ?: 0

            if (selectedDaysInt.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_select_days_for_class), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedEndTime == null) {
                Toast.makeText(this, getString(R.string.toast_select_end_time), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedStartTime!!.timeInMillis >= selectedEndTime!!.timeInMillis) {
                Toast.makeText(this, getString(R.string.toast_start_time_after_end_time), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedRepetitionStartDate == null) {
                Toast.makeText(this, getString(R.string.toast_select_repetition_start_date), Toast.LENGTH_SHORT).show()
                return
            }

            val dates = EventSchedule.occurrences(
                selectedRepetitionStartDate!!.time,
                startTime.format(selectedStartTime!!.time),
                selectedDaysInt,
                repeatWeeks
            ).filter { it.after(Date()) }
            if (dates.isEmpty()) {
                Toast.makeText(this, getString(R.string.invalid_series, EventSchedule.MAX_WEEKS), Toast.LENGTH_LONG).show()
                return
            }
            if (notificacionActivada && !NotificationHelper.ensureExactPermission(this)) return
            val seriesId = UUID.randomUUID().toString()
            val batch = db.batch()
            for (date in dates) {
                val newDocRef = eventsCollectionRef.document()
                val event = CalendarEvent(
                    eventId = newDocRef.id,
                    userId = userId,
                    nombre = eventName,
                    aula = classroom,
                    tipo = EventConstants.TYPE_CLASS,
                    horaInicio = startTime.format(selectedStartTime!!.time),
                    horaFin = startTime.format(selectedEndTime!!.time),
                    diasSemana = selectedDaysInt,
                    repeticiones = repeatWeeks,
                    color = String.format("#%06X", 0xFFFFFF and selectedColor),
                    fecha = date,
                    seriesId = seriesId,
                    notificacionActivada = notificacionActivada,
                    minutosAntes = if (notificacionActivada) minutosAntes else null,
                    activo = true,
                    sonidoAlarmaActivado = sonidoAlarmaActivado,
                    uriSonidoAlarma = uriSonidoAlarma,
                    esSonidoPersonalizado = esSonidoPersonalizado,
                    duracionSonidoPersonalizado = duracionSonidoPersonalizado,
                    vibracionActivada = vibracionActivada,
                    patronVibracion = patronVibracion
                )

                batch.set(newDocRef, event)
            }

            binding.btnSaveEvent.isEnabled = false
            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.toast_classes_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnSaveEvent.isEnabled = true
                    Toast.makeText(this, getString(R.string.toast_error_saving_classes), Toast.LENGTH_SHORT).show()
                    Log.e("AnadirClaseTarea", "Error al guardar clases: ${e.message}", e)
                }

        } else { // Si es una Tarea (ESTO SE MANTIENE SIN CAMBIOS EN REPETICIONES)
            if (selectedEventDate == null) {
                Toast.makeText(this, getString(R.string.toast_select_task_date), Toast.LENGTH_SHORT).show()
                return
            }

            val fechaFinal = EventSchedule.atTime(selectedEventDate!!.time, startTime.format(selectedStartTime!!.time)) ?: return

            if (!fechaFinal.after(Date())) {
                Toast.makeText(this, getString(R.string.toast_task_date_in_past), Toast.LENGTH_SHORT).show()
                return
            }

            if (notificacionActivada && EventSchedule.reminderAt(fechaFinal, startTime.format(selectedStartTime!!.time),
                    minutosAntes, System.currentTimeMillis()) == null) {
                Toast.makeText(this, R.string.reminder_time_passed, Toast.LENGTH_LONG).show()
                return
            }
            if (notificacionActivada && !NotificationHelper.ensureExactPermission(this)) return
            val newDocRef = eventsCollectionRef.document()

            val eventToSave = CalendarEvent(
                eventId = newDocRef.id,
                userId = userId, // ¡NUEVO! Asignar el userId
                nombre = eventName,
                aula = classroom,
                tipo = EventConstants.TYPE_TASK,
                horaInicio = startTime.format(selectedStartTime!!.time),
                horaFin = "",
                diasSemana = emptyList(),
                repeticiones = 0, // Se mantiene en 0 para las tareas
                color = String.format("#%06X", (0xFFFFFF and selectedColor)),
                fecha = fechaFinal,
                notificacionActivada = notificacionActivada,
                minutosAntes = if (notificacionActivada) minutosAntes else null,
                activo = true,
                // --- PASAR LOS NUEVOS CAMPOS DE SONIDO AL EVENTO ---
                sonidoAlarmaActivado = sonidoAlarmaActivado,
                uriSonidoAlarma = uriSonidoAlarma,
                esSonidoPersonalizado = esSonidoPersonalizado,
                duracionSonidoPersonalizado = duracionSonidoPersonalizado,
                // --- PASAR LOS NUEVOS CAMPOS DE VIBRACIÓN AL EVENTO ---
                vibracionActivada = vibracionActivada,
                patronVibracion = patronVibracion
                // ----------------------------------------------------
            )

            binding.btnSaveEvent.isEnabled = false
            newDocRef.set(eventToSave)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.toast_task_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnSaveEvent.isEnabled = true
                    Toast.makeText(this, getString(R.string.toast_error_saving_task), Toast.LENGTH_SHORT).show()
                    Log.e("AnadirClaseTarea", "Error al guardar tarea: ${e.message}", e)
                }
        }
    }


    private fun obtenerDiasSeleccionadosComoInt(): List<Int> {
        val selectedDays = mutableListOf<Int>()
        if (binding.cbMonday.isChecked) selectedDays.add(Calendar.MONDAY)
        if (binding.cbTuesday.isChecked) selectedDays.add(Calendar.TUESDAY)
        if (binding.cbWednesday.isChecked) selectedDays.add(Calendar.WEDNESDAY)
        if (binding.cbThursday.isChecked) selectedDays.add(Calendar.THURSDAY)
        if (binding.cbFriday.isChecked) selectedDays.add(Calendar.FRIDAY)
        if (binding.cbSaturday.isChecked) selectedDays.add(Calendar.SATURDAY)
        if (binding.cbSunday.isChecked) selectedDays.add(Calendar.SUNDAY)
        return selectedDays
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                notificacionActivada = true
                binding.swNotificaciones.isChecked = true
                binding.spinnerReminderTime.visibility = View.VISIBLE
                llSoundOptionsContainer.visibility = View.VISIBLE
                llVibrationOptionsContainer.visibility = View.VISIBLE
                Toast.makeText(this, getString(R.string.toast_permission_granted_reminders_enabled), Toast.LENGTH_SHORT).show()
            } else {
                notificacionActivada = false
                binding.swNotificaciones.isChecked = false
                binding.spinnerReminderTime.visibility = View.GONE
                llSoundOptionsContainer.visibility = View.GONE
                swAlarmSound.isChecked = false
                llVibrationOptionsContainer.visibility = View.GONE
                swVibration.isChecked = false
                Toast.makeText(this, getString(R.string.toast_permission_denied_reminders_disabled), Toast.LENGTH_LONG).show()
            }
        } else {
        }
    }
}