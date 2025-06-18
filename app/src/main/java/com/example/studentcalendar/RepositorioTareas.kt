package com.example.studentcalendar

object TareasRepository {
    private val listaTareas = mutableListOf<Tarea>()

    fun agregarTarea(tarea: Tarea) {
        listaTareas.add(tarea)
    }

    fun obtenerTareas(): List<Tarea> = listaTareas
}
