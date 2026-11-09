package com.example.studentcalendar.ajustes

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.R
import com.example.studentcalendar.databinding.ActivityDatosSincronizadosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.text.SimpleDateFormat
import java.util.*
import com.example.studentcalendar.vertareas.EventConstants

class DatosSincronizadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDatosSincronizadosBinding

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun attachBaseContext(newBase: Context) {
        val contextWithLocale = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDatosSincronizadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSyncDataTitle.text = getString(R.string.sync_data_title)

        val user = auth.currentUser

        if (user != null) {
            val nombre = user.displayName ?: getString(R.string.not_available)
            val email = user.email ?: getString(R.string.not_available)
            val telefono = user.phoneNumber ?: getString(R.string.not_available)
            val proveedor = user.providerData.joinToString { it.providerId }
            val uid = user.uid

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val fechaCreacion = user.metadata?.creationTimestamp?.let {
                dateFormat.format(Date(it))
            } ?: getString(R.string.unknown)

            val ultimoAcceso = user.metadata?.lastSignInTimestamp?.let {
                dateTimeFormat.format(Date(it))
            } ?: getString(R.string.unknown)

            binding.tvPerfil.text = getString(R.string.profile_section_title) + "\n\n" +
                    getString(R.string.profile_name_label, nombre) + "\n" +
                    getString(R.string.profile_email_label, email) + "\n" +
                    getString(R.string.profile_id_label, uid) + "\n" +
                    getString(R.string.profile_provider_label, proveedor) + "\n" +
                    getString(R.string.profile_account_created_label, fechaCreacion) + "\n" +
                    getString(R.string.profile_last_access_label, ultimoAcceso)

            // --- CAMBIO CLAVE AQUÍ: Filtrar por eventos activos ---
            db.collection("users")
                .document(user.uid)
                .collection("events")
                .whereEqualTo("activo", true) // Solo contar eventos activos
                .get(Source.SERVER)
                .addOnSuccessListener { documents ->
                    var totalClases = 0
                    var totalTareas = 0

                    for (doc in documents) {
                        when (doc.getString("tipo")) {
                            EventConstants.TYPE_CLASS -> totalClases++
                            EventConstants.TYPE_TASK -> totalTareas++
                        }
                    }

                    binding.tvClases.text = getString(R.string.classes_synced_count_format, totalClases)
                    binding.tvTareas.text = getString(R.string.tasks_synced_count_format, totalTareas)

                    val fechaActual = dateTimeFormat.format(Date())
                    binding.tvUltimaSync.text = getString(R.string.last_cloud_check, fechaActual)
                }
                .addOnFailureListener {
                    val errorMessage = getString(R.string.error_message_generic)
                    binding.tvClases.text = getString(R.string.classes_synced_error_format, errorMessage)
                    binding.tvTareas.text = getString(R.string.tasks_synced_error_format, errorMessage)
                    binding.tvUltimaSync.text = getString(R.string.last_sync_error_format, errorMessage)
                }

        } else {
            binding.tvPerfil.text = getString(R.string.no_active_session)
            binding.tvClases.text = getString(R.string.classes_synced_count_format, "-")
            binding.tvTareas.text = getString(R.string.tasks_synced_count_format, "-")
            binding.tvUltimaSync.text = getString(R.string.last_cloud_check, "-")
        }
    }
}
