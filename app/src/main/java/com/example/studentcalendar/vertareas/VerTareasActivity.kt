package com.example.studentcalendar.vertareas

import com.example.studentcalendar.notificaciones.ReminderStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentcalendar.databinding.ActivityVerTareasBinding // Importa tu binding específico
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.example.studentcalendar.inicio.CalendarEventAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import android.content.Context
import com.example.studentcalendar.R
import com.example.studentcalendar.ajustes.LocaleHelper
import java.util.Calendar
import java.util.Date
import android.util.Log
import com.example.studentcalendar.base.BaseActivity
import com.google.android.material.snackbar.Snackbar

class VerTareasActivity : BaseActivity<ActivityVerTareasBinding>() {

    override fun getViewBinding(): ActivityVerTareasBinding {
        return ActivityVerTareasBinding.inflate(layoutInflater)
    }

    override fun getNavigationMenuItemId(): Int = R.id.nav_view_tasks

    private lateinit var adapter: CalendarEventAdapter
    private var listaEventosCompleta = listOf<CalendarEvent>()
    private var listaEventosFiltrada = mutableListOf<CalendarEvent>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Corrección de sintaxis
    private var listenerRegistro: ListenerRegistration? = null

    private val editarEventoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // ¡IMPORTANTE! Llama a super.onCreate primero.

        binding.tvTitulo.text = getString(R.string.title_your_tasks_and_classes)
        binding.searchView.queryHint = getString(R.string.search_hint_event_name)
        binding.tituloNotificaciones.text = getString(R.string.title_active_reminders)
        binding.textoExplicativo.text = getString(R.string.explanation_reminders)
        binding.tvSinEventos.text = getString(R.string.no_events_registered)
        binding.tvSinEventos.visibility = View.VISIBLE

        val searchText = binding.searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(getColor(R.color.kalendu_text))
        searchText.setHintTextColor(getColor(R.color.kalendu_text))

        adapter = CalendarEventAdapter(
            listaEventosFiltrada,
            onItemClick = { evento ->
                val intent = Intent(this, EditarEventoActivity::class.java)
                intent.putExtra("eventoId", evento.eventId)
                editarEventoLauncher.launch(intent)
            },
            onDeleteClick = { evento ->
                if (isNetworkAvailable()) {
                    mostrarDialogoConfirmacionEliminar(evento)
                } else {
                    showSnackbar(binding.root, getString(R.string.no_connection_delete_event)) // Nuevo string
                }
            }
        )

        binding.rvEventos.layoutManager = LinearLayoutManager(this)
        binding.rvEventos.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText?.lowercase()?.trim() ?: ""
                listaEventosFiltrada.clear()
                listaEventosFiltrada.addAll(
                    listaEventosCompleta.filter {
                        it.activo && it.nombre.lowercase().contains(texto)
                    }
                )
                adapter.notifyDataSetChanged()

                if (listaEventosFiltrada.isEmpty()) {
                    binding.tvSinEventos.text = if (listaEventosCompleta.isEmpty()) {
                        if (!isNetworkAvailable()) getString(R.string.sin_eventos_internet)
                        else getString(R.string.no_events_registered)
                    } else {
                        getString(R.string.no_events_matching_search) // Nuevo string
                    }
                    binding.tvSinEventos.visibility = View.VISIBLE
                } else {
                    binding.tvSinEventos.visibility = View.GONE
                }
                return true
            }
        })

        cargarEventosDesdeFirestore()
    }

    override fun onResume() {
        super.onResume() // ¡IMPORTANTE! Llama a super.onResume para que BaseActivity actualice la BottomNavigationView
        // Si tuvieras lógica de `onResume` específica de VerTareasActivity que no esté cubierta
        // por adjustUIBasedOnNetwork(), iría aquí.
    }

    // Sobreescribe adjustUIBasedOnNetwork para ajustes específicos de la UI de VerTareasActivity
    override fun adjustUIBasedOnNetwork() {
        super.adjustUIBasedOnNetwork() // Llama a la implementación de BaseActivity primero

        val isConnected = isNetworkAvailable()

        // Si tienes otros botones o elementos de UI en VerTareasActivity que dependan de internet,
        // puedes deshabilitarlos/habilitarlos aquí. Ejemplo:
        // val onlineButtons: List<View> = listOf(binding.btnSyncSpecificTasks)
        // toggleViewsEnabled(onlineButtons, isConnected)

        // Ajusta el texto de tvSinEventos si la lista completa está vacía y la conexión cambia
        if (listaEventosCompleta.isEmpty()) {
            if (!isConnected) {
                binding.tvSinEventos.text = getString(R.string.sin_eventos_internet)
                binding.tvSinEventos.visibility = View.VISIBLE
            } else {
                binding.tvSinEventos.text = getString(R.string.no_events_registered)
                binding.tvSinEventos.visibility = View.VISIBLE
            }
        } else if (listaEventosFiltrada.isEmpty() && binding.searchView.query?.isNotBlank() == true) {
            // Si hay eventos pero el filtro de búsqueda los deja vacíos
            binding.tvSinEventos.text = getString(R.string.no_events_matching_search)
            binding.tvSinEventos.visibility = View.VISIBLE
        } else if (listaEventosFiltrada.isNotEmpty()){
            binding.tvSinEventos.visibility = View.GONE
        }
    }


    private fun cargarEventosDesdeFirestore() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            binding.tvSinEventos.text = getString(R.string.user_not_authenticated)
            binding.tvSinEventos.visibility = View.VISIBLE
            return
        }

        listenerRegistro = db.collection("users")
            .document(userId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VerTareasActivity", "Error al cargar eventos: ${error.message}", error)
                    if (!isNetworkAvailable()) {
                        binding.tvSinEventos.text = getString(R.string.sin_eventos_internet)
                    } else {
                        binding.tvSinEventos.text = getString(R.string.error_loading_events)
                    }
                    binding.tvSinEventos.visibility = View.VISIBLE

                    listaEventosCompleta = emptyList() // Limpia los datos si hay un error persistente
                    listaEventosFiltrada.clear()
                    adapter.notifyDataSetChanged()
                    return@addSnapshotListener
                }

                val documentos = snapshot?.documents ?: emptyList()
                val eventosCrudos = documentos.mapNotNull { it.toObject(CalendarEvent::class.java) }
                val eventosAActualizar = mutableListOf<Pair<String, Map<String, Any>>>()

                val currentTime = Calendar.getInstance()

                val eventosParaMostrar = mutableListOf<CalendarEvent>()

                for (event in eventosCrudos) {
                    if (!event.activo) {
                        Log.d("VerTareasActivity", "Evento ${event.nombre} (ID: ${event.eventId}) ya está inactivo. No se mostrará.")
                        continue
                    }

                    var isValidTimeFormat = true
                    val eventCalendar = Calendar.getInstance().apply {
                        time = event.fecha ?: Date() // Provee un default si 'fecha' es nulo, aunque debería estar en el modelo
                        val horaPartes = event.horaInicio.split(":")
                        try {
                            set(Calendar.HOUR_OF_DAY, horaPartes[0].toInt())
                            set(Calendar.MINUTE, horaPartes[1].toInt())
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        } catch (e: NumberFormatException) {
                            Log.e("VerTareasActivity", "Error de formato de hora para evento ${event.eventId}: ${event.horaInicio}", e)
                            eventosAActualizar.add(Pair(event.eventId, mapOf("activo" to false)))
                            isValidTimeFormat = false
                        } catch (e: Exception) { // Captura otros posibles errores al parsear
                            Log.e("VerTareasActivity", "Error inesperado al parsear hora para evento ${event.eventId}: ${event.horaInicio}", e)
                            eventosAActualizar.add(Pair(event.eventId, mapOf("activo" to false)))
                            isValidTimeFormat = false
                        }
                    }

                    if (!isValidTimeFormat) {
                        continue
                    }

                    if (eventCalendar.timeInMillis <= currentTime.timeInMillis) {
                        Log.d("VerTareasActivity", "Evento ${event.nombre} (ID: ${event.eventId}) ha pasado. Marcando como inactivo.")
                        eventosAActualizar.add(Pair(event.eventId, mapOf("activo" to false)))
                    } else {
                        eventosParaMostrar.add(event)
                    }
                }

                if (eventosAActualizar.isNotEmpty()) {
                    // Solo intentar la actualización por lotes si hay conexión
                    if (isNetworkAvailable()) {
                        val batch = db.batch()
                        for ((eventIdToUpdate, dataToUpdate) in eventosAActualizar) {
                            val docRef = db.collection("users").document(userId).collection("events").document(eventIdToUpdate)
                            batch.update(docRef, dataToUpdate)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("VerTareasActivity", "Batch de actualización de eventos inactivos completado.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("VerTareasActivity", "Error al ejecutar batch de actualización de eventos inactivos: ${e.message}", e)
                                Toast.makeText(this, getString(R.string.error_updating_events_offline), Toast.LENGTH_SHORT).show() // Nuevo string
                            }
                    } else {
                        Log.w("VerTareasActivity", "No hay conexión para ejecutar batch de actualización de eventos inactivos. Se intentará más tarde.")
                        // Firestore manejará la sincronización cuando la conexión se restablezca.
                    }
                }

                listaEventosCompleta = eventosParaMostrar.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                val currentQuery = binding.searchView.query?.toString()?.lowercase()?.trim() ?: ""
                listaEventosFiltrada.clear()
                listaEventosFiltrada.addAll(
                    listaEventosCompleta.filter {
                        it.nombre.lowercase().contains(currentQuery)
                    }
                )

                adapter.notifyDataSetChanged()
                // Actualiza la visibilidad y el texto de tvSinEventos después de cargar/filtrar
                if (listaEventosFiltrada.isEmpty()) {
                    if (listaEventosCompleta.isEmpty()) { // Si no se cargó nada o la lista completa está vacía
                        if (!isNetworkAvailable()) getString(R.string.sin_eventos_internet)
                        else getString(R.string.no_events_registered)
                    } else { // Si hay eventos en listaCompleta pero el filtro los elimina
                        getString(R.string.no_events_matching_search)
                    }.also { binding.tvSinEventos.text = it }
                    binding.tvSinEventos.visibility = View.VISIBLE
                } else {
                    binding.tvSinEventos.visibility = View.GONE
                }
            }
    }

    private fun mostrarDialogoConfirmacionEliminar(eventoAEliminar: CalendarEvent) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            return
        }
        val ref = db.collection("users").document(userId).collection("events")

        // Los métodos addOnSuccessListener y addOnFailureListener se benefician de las lambdas más limpias.
        val deleteSuccessListener = { _: Void? -> // <-- Ahora acepta un argumento (que ignoramos con '_')
            ReminderStore(this).remove(userId, listOf(eventoAEliminar.eventId))
            Toast.makeText(this, R.string.event_deleted, Toast.LENGTH_SHORT).show()
        }
        val deleteFailureListener = { e: Exception ->
            Toast.makeText(this, "${getString(R.string.error_deleting_event)}: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("VerTareasActivity", "Error al eliminar evento ${eventoAEliminar.eventId}: ${e.message}", e)
            Unit // <--- ¡Añade esto! Asegura que la lambda devuelve Unit
        }

        if (eventoAEliminar.tipo == EventConstants.TYPE_CLASS && eventoAEliminar.seriesId.isNotBlank()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_class_options_title))
                .setMessage(getString(R.string.delete_class_options_message))
                .setPositiveButton(getString(R.string.delete_this_class_only)) { _, _ ->
                    // La verificación de red ya se hizo en onDeleteClick antes de llamar a este diálogo
                    ref.document(eventoAEliminar.eventId).delete()
                        .addOnSuccessListener(deleteSuccessListener)
                        .addOnFailureListener(deleteFailureListener)
                }
                .setNegativeButton(getString(R.string.delete_all_repeated_classes)) { _, _ ->
                    // Verificación de red adicional para la operación de eliminación de todas las repetidas
                    if (!isNetworkAvailable()) {
                        showSnackbar(binding.root, getString(R.string.no_connection_delete_event))
                        return@setNegativeButton // Impide la acción si no hay internet
                    }
                    ref.whereEqualTo("seriesId", eventoAEliminar.seriesId)
                        .get()
                        .addOnSuccessListener { result ->
                            val batch = db.batch()
                            for (document in result.documents) {
                                batch.delete(document.reference)
                            }
                            batch.commit()
                                .addOnSuccessListener {
                                    ReminderStore(this).remove(userId, result.documents.map { it.id })
                                    Toast.makeText(this, R.string.repeated_classes_deleted, Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "${getString(R.string.error_deleting_repeated_classes)}: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("VerTareasActivity", "Error al eliminar clases repetidas: ${e.message}", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "${getString(R.string.error_loading_classes_to_delete)}: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("VerTareasActivity", "Error al cargar clases para eliminar: ${e.message}", e)
                        }
                }
                .setNeutralButton(getString(R.string.cancel_action), null)
                .show()
        } else { // Para TAREAS (TYPE_TASK)
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_event_confirmation_title))
                .setMessage(getString(R.string.delete_event_confirmation_message2, eventoAEliminar.nombre))
                .setPositiveButton(getString(R.string.yes_delete)) { _, _ ->
                    ref.document(eventoAEliminar.eventId).delete()
                        .addOnSuccessListener(deleteSuccessListener)
                        .addOnFailureListener(deleteFailureListener)
                }
                .setNegativeButton(getString(R.string.cancel_action), null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistro?.remove()
    }
}