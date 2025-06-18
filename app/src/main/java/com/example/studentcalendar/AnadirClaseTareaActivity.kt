package com.example.studentcalendar

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AnadirClaseTareaActivity : AppCompatActivity() {

    // Bloque de variables de interfaz
    private lateinit var spinnerTipo: Spinner
    private lateinit var layoutCurso: View
    private lateinit var layoutTarea: View

    // Curso
    private lateinit var etNombreCurso: TextInputEditText
    private lateinit var etAulaCurso: TextInputEditText
    private lateinit var btnHoraInicio: Button
    private lateinit var btnHoraFin: Button
    private lateinit var spinnerColorCurso: Spinner

    // Tarea
    private lateinit var etTituloTarea: TextInputEditText
    private lateinit var etDescripcionTarea: TextInputEditText
    private lateinit var btnHoraTarea: Button
    private lateinit var spinnerCursoRelacionado: Spinner

    // Otros
    private lateinit var btnGuardar: Button
    private lateinit var btnCerrar: Button

    // Datos temporales de hora
    private var horaInicio = ""
    private var horaFin = ""
    private var horaTarea = ""

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anadir_clase_tarea)

        // Referencias UI
        spinnerTipo = findViewById(R.id.spinnerTipo)
        layoutCurso = findViewById(R.id.layoutCurso)
        layoutTarea = findViewById(R.id.layoutTarea)

        etNombreCurso = findViewById(R.id.etNombreCurso)
        etAulaCurso = findViewById(R.id.etAulaCurso)
        btnHoraInicio = findViewById(R.id.btnHoraInicio)
        btnHoraFin = findViewById(R.id.btnHoraFin)
        spinnerColorCurso = findViewById(R.id.spinnerColorCurso)

        etTituloTarea = findViewById(R.id.etTituloTarea)
        etDescripcionTarea = findViewById(R.id.etDescripcionTarea)
        btnHoraTarea = findViewById(R.id.btnHoraTarea)
        spinnerCursoRelacionado = findViewById(R.id.spinnerCursoRelacionado)

        btnGuardar = findViewById(R.id.btnGuardar)
        btnCerrar = findViewById(R.id.btnCerrar)

        // Ocultar formularios al inicio
        layoutCurso.visibility = View.GONE
        layoutTarea.visibility = View.GONE

        // Selector inicial entre curso o tarea
        val opciones = arrayOf("Curso", "Tarea")
        val selectorDialogo = android.app.AlertDialog.Builder(this)
        selectorDialogo.setTitle("¿Qué deseas añadir?")
        selectorDialogo.setItems(opciones) { _, which ->
            spinnerTipo.setSelection(which)
        }
        selectorDialogo.setCancelable(false)
        selectorDialogo.show()

        // Spinner tipo
        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opciones)
        spinnerTipo.adapter = adapterTipos

        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (opciones[pos] == "Curso") {
                    layoutCurso.visibility = View.VISIBLE
                    layoutTarea.visibility = View.GONE
                } else {
                    layoutCurso.visibility = View.GONE
                    layoutTarea.visibility = View.VISIBLE
                    cargarCursosEnSpinner()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Spinner colores
        val colores = listOf("Rojo", "Verde", "Azul", "Amarillo", "Morado")
        val adapterColores = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colores)
        spinnerColorCurso.adapter = adapterColores

        // Botón de cierre
        btnCerrar.setOnClickListener { finish() }

        // Botones de hora
        btnHoraInicio.setOnClickListener {
            mostrarTimePicker { hora ->
                horaInicio = hora
                btnHoraInicio.text = "Inicio: $hora"
            }
        }

        btnHoraFin.setOnClickListener {
            mostrarTimePicker { hora ->
                horaFin = hora
                btnHoraFin.text = "Fin: $hora"
            }
        }

        btnHoraTarea.setOnClickListener {
            mostrarTimePicker { hora ->
                horaTarea = hora
                btnHoraTarea.text = "Hora: $hora"
            }
        }

        // Botón guardar
        btnGuardar.setOnClickListener {
            val tipo = spinnerTipo.selectedItem.toString()
            if (tipo == "Curso") guardarCurso() else guardarTarea()
        }
    }

    private fun mostrarTimePicker(onHoraSeleccionada: (String) -> Unit) {
        val calendario = Calendar.getInstance()
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minuto = calendario.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(this, { _, hourOfDay, minuteOfHour ->
            val horaFormateada = String.format("%02d:%02d", hourOfDay, minuteOfHour)
            onHoraSeleccionada(horaFormateada)
        }, hora, minuto, true)

        timePicker.show()
    }

    private fun guardarCurso() {
        val nombre = etNombreCurso.text.toString().trim()
        val aula = etAulaCurso.text.toString().trim()
        val color = spinnerColorCurso.selectedItem.toString()

        if (nombre.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos del curso", Toast.LENGTH_SHORT).show()
            return
        }

        val curso = hashMapOf(
            "nombre" to nombre,
            "aula" to aula,
            "color" to color,
            "horaInicio" to horaInicio,
            "horaFin" to horaFin
        )

        firestore.collection("cursos")
            .add(curso)
            .addOnSuccessListener {
                Toast.makeText(this, "Curso guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar el curso", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarTarea() {
        val titulo = etTituloTarea.text.toString().trim()
        val descripcion = etDescripcionTarea.text.toString().trim()
        val cursoRelacionado = spinnerCursoRelacionado.selectedItem?.toString() ?: ""

        if (titulo.isEmpty() || horaTarea.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos de la tarea", Toast.LENGTH_SHORT).show()
            return
        }

        val tarea = hashMapOf(
            "titulo" to titulo,
            "descripcion" to descripcion,
            "hora" to horaTarea,
            "cursoRelacionado" to cursoRelacionado
        )

        firestore.collection("tareas")
            .add(tarea)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea guardada correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar la tarea", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarCursosEnSpinner() {
        firestore.collection("cursos")
            .get()
            .addOnSuccessListener { result ->
                val nombres = result.map { it.getString("nombre") ?: "Sin nombre" }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
                spinnerCursoRelacionado.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar cursos", Toast.LENGTH_SHORT).show()
            }
    }
}
