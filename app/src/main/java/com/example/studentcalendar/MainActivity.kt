package com.example.studentcalendar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studentcalendar.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido
                Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Permiso de POST_NOTIFICATIONS concedido por el usuario")
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de notificaciones denegado. Las alarmas no se mostrarán.", Toast.LENGTH_LONG).show()
                Log.w("MainActivity", "Permiso de POST_NOTIFICATIONS denegado por el usuario")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /*
                auth = FirebaseAuth.getInstance()
                val user = auth.currentUser

<<<<<<< HEAD
                // ✅ VERIFICAR SI EL USUARIO ESTÁ AUTENTICADO
                if (user == null) {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return
                }
=======
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        

        if (user == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
>>>>>>> 55e5273d9bb8751daa9c5ae3468a2a61699e390d

        val displayName = user.displayName ?: "Usuario"
        binding.tvUserName.text = "👋 Hola, $displayName"

        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.user) // Imagen por defecto si no carga
                .circleCrop()
                .into(binding.imgProfile)
        } else {
            binding.imgProfile.setImageResource(R.drawable.user)
        }*/

        val fechaActual = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(Date())
        binding.tvTodayDate.text = "📅 Hoy es: $fechaActual"

        binding.tvNextClass.text = "🕒 Próxima clase: Álgebra"
        binding.tvNextClassRoom.text = "🏫 Aula B201 – 10:00 AM"

        binding.btnAddTask.setOnClickListener {
            val intent = Intent(this, AnadirClaseTareaActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewSchedule.setOnClickListener {
            startActivity(Intent(this, HorarioSemanalActivity::class.java))
        }

        binding.btnViewTasks.setOnClickListener {
            startActivity(Intent(this, VerTareasActivity::class.java))
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }

        binding.btnExportPDF.setOnClickListener {
            startActivity(Intent(this, ExportarHorarioPdfActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, AjustesActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Cerrar sesión")
            builder.setMessage("¿Estás seguro de que deseas cerrar sesión?")

            builder.setPositiveButton("Sí") { _, _ ->
                auth.signOut()
                Identity.getSignInClient(this).signOut().addOnCompleteListener {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }
}
