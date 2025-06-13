package com.example.studentcalendar

import com.bumptech.glide.Glide
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.identity.Identity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        // ✅ VERIFICAR SI EL USUARIO ESTÁ AUTENTICADO
        if (user == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // ✅ MOSTRAR NOMBRE DE USUARIO
        val displayName = user.displayName ?: "Usuario"
        binding.tvUserName.text = "👋 Hola, $displayName"

        // ✅ MOSTRAR FOTO DE PERFIL (si existe)
        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.user) // Imagen por defecto si no carga
                .circleCrop()
                .into(binding.imgProfile)
        } else {
            binding.imgProfile.setImageResource(R.drawable.user)
        }

        // ✅ FECHA ACTUAL EN ESPAÑOL
        val fechaActual = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(Date())
        binding.tvTodayDate.text = "📅 Hoy es: $fechaActual"

        // ✅ EJEMPLO ESTÁTICO DE PRÓXIMA CLASE
        binding.tvNextClass.text = "🕒 Próxima clase: Álgebra"
        binding.tvNextClassRoom.text = "🏫 Aula B201 – 10:00 AM"

        // ✅ INTENTS DE BOTONES
        binding.btnAddTask.setOnClickListener {
            startActivity(Intent(this, AnadirClaseTareaActivity::class.java))
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

        // ✅ BOTÓN CERRAR SESIÓN
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
