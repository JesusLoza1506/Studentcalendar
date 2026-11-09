package com.example.studentcalendar.inicio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.example.studentcalendar.vertareas.EventConstants
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.studentcalendar.AnadirClaseTareaActivity
import com.example.studentcalendar.ExportarHorarioPdfActivity
import com.example.studentcalendar.R
import com.example.studentcalendar.vertareas.VerTareasActivity
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.databinding.ActivityMainBinding
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.example.studentcalendar.horariosemanal.HorarioSemanalActivity
import com.example.studentcalendar.notificaciones.NotificacionesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.text.DateFormat
import com.example.studentcalendar.ajustes.AjustesActivity
import com.example.studentcalendar.base.BaseActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun getNavigationMenuItemId(): Int = R.id.nav_home


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val REQUEST_CODE_POST_NOTIFICATIONS = 2001


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val user = auth.currentUser

        if (user == null) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        val userId = user.uid
        val sharedPrefs = getSharedPreferences("kalendu_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("userId", userId).apply()

        val fechaActualFormateada = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date())
        binding.tvTodayDate.text = getString(R.string.hoy_es, fechaActualFormateada)

        binding.tvNextClassTitle.text = getString(R.string.alerta_evento)

        binding.btnAddTask.text = getString(R.string.btn_nueva_actividad)

        binding.btnAddTask.setOnClickListener {

            startActivity(Intent(this, AnadirClaseTareaActivity::class.java))
        }

        binding.imgSettings.setOnClickListener {
            startActivity(Intent(this, AjustesActivity::class.java))
        }

        actualizarUsuario()
        solicitarPermisosNotificaciones()
        crearCanalNotificaciones()
    }

    override fun onResume() {
        super.onResume()
        actualizarUsuario()
        cargarProximoEvento()
        val fechaActualFormateada = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date())
        binding.tvTodayDate.text = getString(R.string.hoy_es, fechaActualFormateada)
    }

    override fun adjustUIBasedOnNetwork() {
        super.adjustUIBasedOnNetwork() // Llama a la implementación de BaseActivity

        val onlineButtonsInMainActivity: List<View> = listOf(
        )
        val isConnected = isNetworkAvailable()
        toggleViewsEnabled(onlineButtonsInMainActivity, isConnected)
    }


    private fun actualizarUsuario() {
        val user = auth.currentUser ?: return

        val prefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val localUri = prefs.getString("localPhotoUri_${user.uid}", null)

        val displayName = user.displayName ?: getString(R.string.usuario)
        binding.tvUserName.text = getString(R.string.hola_usuario, displayName)

        if (localUri != null) {
            Glide.with(this)
                .load(Uri.parse(localUri))
                .placeholder(R.drawable.user)
                .circleCrop()
                .into(binding.imgProfile)
        } else if (user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.user)
                .circleCrop()
                .into(binding.imgProfile)
        } else {
            binding.imgProfile.setImageResource(R.drawable.user)
        }

    }

    private fun solicitarPermisosNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "kalendu_channel"
            val nombre = "Notificaciones KalendU"
            val descripcion = "Recordatorios de clases y tareas"
            val importancia = android.app.NotificationManager.IMPORTANCE_HIGH
            val canal = android.app.NotificationChannel(channelId, nombre, importancia).apply {
                description = descripcion
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(canal)
        }
    }

    private fun cargarProximoEvento() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("events")
            .whereGreaterThan("fecha", Date())
            .orderBy("fecha")
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    binding.tvNextClassTitle.text = getString(R.string.alerta_evento)
                    binding.tvNextClass.text = getString(R.string.sin_eventos)
                    binding.tvNextClassRoom.text = ""
                    return@addOnSuccessListener
                }

                val ahora = Date()

                val eventosFuturos = docs.mapNotNull { it.toObject(CalendarEvent::class.java) }
                    .filter { it.fecha.after(ahora) }
                    .sortedBy { it.fecha }

                if (eventosFuturos.isEmpty()) {
                    binding.tvNextClassTitle.text = getString(R.string.alerta_evento)
                    binding.tvNextClass.text = getString(R.string.sin_eventos)
                    binding.tvNextClassRoom.text = ""
                    return@addOnSuccessListener
                }

                val proximo = eventosFuturos.first()

                binding.tvNextClassTitle.text = getString(R.string.proximo_evento)
                binding.tvNextClass.text = proximo.nombre

                val horaInicio = proximo.horaInicio ?: ""
                val horaFin = proximo.horaFin ?: ""
                val aula = proximo.aula ?: "-"
                val hours = if (proximo.tipo == EventConstants.TYPE_TASK) horaInicio else "$horaInicio – $horaFin"
                binding.tvNextClassRoom.text = listOf(hours, aula).filter { it.isNotBlank() }.joinToString(" · ")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar próximos eventos: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error cargando eventos", it)
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permiso de notificación concedido")
            } else {
                Log.d("MainActivity", "Permiso de notificación denegado")
            }
        }
    }
}