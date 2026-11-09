package com.example.studentcalendar.notificaciones

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.studentcalendar.R
import com.example.studentcalendar.inicio.MainActivity
import java.util.concurrent.ConcurrentHashMap

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var eventId: String? = null

    private val TAG = "AlarmService"

    private lateinit var vibrationPatterns: ConcurrentHashMap<String, LongArray>

    companion object {
        private const val FOREGROUND_SERVICE_ID = 1001
        private const val CHANNEL_ID = "kalendu_alarm_channel_id"
        private const val CHANNEL_NAME = "Alarma de Evento KalendU"
        private const val CHANNEL_DESC = "Reproduce el sonido y la vibración de la alarma de tu evento."

        const val EXTRA_EVENT_ID = "EVENT_ID"
        const val  EXTRA_EVENT_NAME = "EVENT_NAME"
        const val EXTRA_EVENT_TYPE = "EVENT_TYPE"
        const val EXTRA_EVENT_CLASSROOM = "EVENT_CLASSROOM"
        const val EXTRA_SOUND_ENABLED = "SOUND_ENABLED"
        const val EXTRA_SOUND_URI = "SOUND_URI"
        const val EXTRA_IS_CUSTOM_SOUND = "IS_CUSTOM_SOUND"
        const val EXTRA_CUSTOM_SOUND_DURATION = "CUSTOM_SOUND_DURATION"
        const val EXTRA_VIBRATION_ENABLED = "VIBRATION_ENABLED"
        const val EXTRA_VIBRATION_PATTERN_NAME = "VIBRATION_PATTERN_NAME"

        const val ACTION_STOP_ALARM = "STOP_ALARM_ACTION"
    }

    private val alarmCallbacks = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio de alarma creado.")
        setupVibrationPatterns()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ejecutado.")

        if (intent?.action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Acción STOP_ALARM_ACTION recibida. Deteniendo alarma y servicio.")
            stopAlarmAndService()
            return START_NOT_STICKY
        }

        if (intent == null) {
            Log.w(TAG, "Intent nulo en onStartCommand, deteniendo servicio.")
            stopSelf()
            return START_NOT_STICKY
        }

        eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: "Recordatorio"
        val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: "evento"
        val classroom = intent.getStringExtra(EXTRA_EVENT_CLASSROOM) ?: ""
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, false)
        val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI)
        val isCustomSound = intent.getBooleanExtra(EXTRA_IS_CUSTOM_SOUND, false)
        val customSoundDuration = intent.getLongExtra(EXTRA_CUSTOM_SOUND_DURATION, 0L)
        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, false)
        val vibrationPatternName = intent.getStringExtra(EXTRA_VIBRATION_PATTERN_NAME)

        Log.d(TAG, "Datos de alarma recibidos para $eventName (ID: $eventId):")
        Log.d(TAG, "Sonido: $soundEnabled, URI: ${soundUriString ?: "N/A"}, Personalizado: $isCustomSound, Duración: $customSoundDuration")
        Log.d(TAG, "Vibración: $vibrationEnabled, Patrón: ${vibrationPatternName ?: "N/A"}")

        val notification = createForegroundNotification(eventName, eventType, classroom)
        startForeground(FOREGROUND_SERVICE_ID, notification)
        Log.d(TAG, "Servicio iniciado como Foreground Service con ID: $FOREGROUND_SERVICE_ID.")

        stopAlarmResources()

        startAlarmResources(soundEnabled, soundUriString, isCustomSound, customSoundDuration, vibrationEnabled, vibrationPatternName)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESC
                    setSound(null, null)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Canal de notificación '$CHANNEL_NAME' creado para AlarmService.")
            } else {
                Log.d(TAG, "Canal de notificación '$CHANNEL_NAME' ya existe.")
            }
        }
    }

    private fun createForegroundNotification(eventName: String, eventType: String, classroom: String): Notification {
        val displayEventType = when (eventType.lowercase()) {
            "class" -> getString(R.string.event_type_class_display)
            "task" -> getString(R.string.event_type_task_display)
            else -> eventType
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_event_id", eventId)
        }

        val pendingContentIntent = PendingIntent.getActivity(
            this,
            eventId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopSelfIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            eventId.hashCode() + 1,
            stopSelfIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_add_alert_24)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(getString(R.string.alarm_notification_content, displayEventType, eventName, if (classroom.isNotBlank()) "en $classroom" else ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.outline_alarm_off_24, getString(R.string.alarm_stop_button), stopPendingIntent)
            .build()
    }

    private fun setupVibrationPatterns() {
        vibrationPatterns = ConcurrentHashMap<String, LongArray>().apply {
            put("short", longArrayOf(0, 200))
            put(getString(R.string.vibration_pattern_short), getValue("short"))
            put("long", longArrayOf(0, 500))
            put(getString(R.string.vibration_pattern_long), getValue("long"))
            put("double_short", longArrayOf(0, 150, 100, 150))
            put(getString(R.string.vibration_pattern_double_short), getValue("double_short"))
            put("heartbeat", longArrayOf(0, 100, 50, 100, 200, 100, 50, 100))
            put(getString(R.string.vibration_pattern_heartbeat), getValue("heartbeat"))
        }
        Log.d(TAG, "Patrones de vibración inicializados.")
    }

    private fun startAlarmResources(soundEnabled: Boolean, soundUriString: String?, isCustomSound: Boolean, customSoundDuration: Long,
                                    vibrationEnabled: Boolean, vibrationPatternName: String?) {

        if (soundEnabled) {
            try {
                val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val soundUri = soundUriString?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: defaultSound
                val player = MediaPlayer()
                mediaPlayer = player
                player.apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                    try {
                        setDataSource(applicationContext, soundUri)
                    } catch (error: Exception) {
                        reset()
                        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                        setDataSource(applicationContext, defaultSound)
                    }
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.start()
                        Log.d(TAG, "Reproduciendo sonido para $eventId: $soundUri (en bucle)")

                        val maxSoundDuration = if (isCustomSound && customSoundDuration > 0) {
                            customSoundDuration
                        } else {
                            15000L
                        }
                        alarmCallbacks.postDelayed({
                            if (mediaPlayer?.isPlaying == true) {
                                stopSound()
                                Log.d(TAG, "Sonido para $eventId detenido automáticamente después de ${maxSoundDuration / 1000} segundos.")
                            }
                        }, maxSoundDuration)
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "Error al reproducir sonido para $eventId: what=$what, extra=$extra")
                        stopSound()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al iniciar MediaPlayer para $eventId: ${e.message}", e)
                stopSound()
            }
        } else {
            Log.d(TAG, "Sonido no activado o URI nula/vacía.")
        }

        if (vibrationEnabled) {
            val pattern = vibrationPatterns[vibrationPatternName]
            if (pattern != null) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                        vibrator?.vibrate(vibrationEffect)
                        Log.d(TAG, "Iniciando vibración para $eventId con patrón: ${pattern.joinToString()}")
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(pattern, 0)
                        Log.d(TAG, "Iniciando vibración (deprecated) para $eventId con patrón: ${pattern.joinToString()}")
                    }

                    val maxVibrationDuration = 15000L
                    alarmCallbacks.postDelayed({
                        stopVibration()
                        Log.d(TAG, "Vibración para $eventId detenida automáticamente después de ${maxVibrationDuration / 1000} segundos.")
                    }, maxVibrationDuration)

                } else {
                    Log.w(TAG, "El dispositivo no soporta vibración o vibrator es nulo.")
                }
            } else {
                Log.d(TAG, "Patrón de vibración nulo o no encontrado para: $vibrationPatternName")
            }
        } else {
            Log.d(TAG, "Vibración no activada.")
        }
    }

    private fun stopAlarmAndService() {
        Log.d(TAG, "stopAlarmAndService llamado.")
        stopAlarmResources()
        stopSelf()
    }

    private fun stopAlarmResources() {
        alarmCallbacks.removeCallbacksAndMessages(null)
        stopSound()
        stopVibration()
        Log.d(TAG, "Recursos de alarma (sonido/vibración) detenidos.")
    }

    private fun stopSound() {
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "Sonido detenido y MediaPlayer liberado.")
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
        Log.d(TAG, "Vibración detenida.")
    }

    override fun onDestroy() {
        Log.d(TAG, "Servicio de alarma destruido.")
        stopAlarmResources()
        super.onDestroy()
    }
}