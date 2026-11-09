package com.example.studentcalendar.horariosemanal

import java.util.Date

data class CalendarEvent(
    var eventId: String = "",
    var userId: String = "",
    var nombre: String = "",
    var tipo: String = "",
    var fecha: Date = Date(),
    var horaInicio: String = "",
    var horaFin: String = "",
    var aula: String = "",
    var color: String = "",
    var activo: Boolean = true,
    var repeticiones: Int = 0,
    var diasSemana: List<Int> = listOf(),
    var notificacionActivada: Boolean = false,
    var minutosAntes: Int? = null,
    var sonidoAlarmaActivado: Boolean = false,
    var uriSonidoAlarma: String? = null,
    var esSonidoPersonalizado: Boolean = false,
    var duracionSonidoPersonalizado: Long? = null,
    var vibracionActivada: Boolean = false,
    var patronVibracion: String? = null,
    var seriesId: String = ""
)
