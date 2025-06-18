package com.example.studentcalendar

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class HorarioSemanalActivity : AppCompatActivity() {

    private lateinit var headerDays: LinearLayout
    private lateinit var hourLabels: LinearLayout
    private lateinit var gridHorario: GridLayout

    private lateinit var headerScroll: HorizontalScrollView
    private lateinit var bodyScroll: HorizontalScrollView
    private lateinit var hourScroll: ScrollView
    private lateinit var bodyVerticalScroll: ScrollView

    private val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horario_semanal)

        // Referencias a vistas
        headerDays = findViewById(R.id.headerDays)
        hourLabels = findViewById(R.id.hourLabels)
        gridHorario = findViewById(R.id.gridHorario)

        headerScroll = findViewById(R.id.headerScroll)
        bodyScroll = findViewById(R.id.bodyScroll)
        hourScroll = findViewById(R.id.hourScroll)
        bodyVerticalScroll = findViewById(R.id.bodyVerticalScroll)

        sincronizarScrolls()

        gridHorario.columnCount = diasSemana.size
        gridHorario.rowCount = 24

        agregarEncabezadosDeDias()
        agregarHoras()
        generarCeldasVacias()
    }

    private fun sincronizarScrolls() {
        bodyScroll.viewTreeObserver.addOnScrollChangedListener {
            headerScroll.scrollTo(bodyScroll.scrollX, 0)
        }

        bodyVerticalScroll.viewTreeObserver.addOnScrollChangedListener {
            hourScroll.scrollTo(0, bodyVerticalScroll.scrollY)
        }
    }

    private fun agregarEncabezadosDeDias() {
        for (dia in diasSemana) {
            val textView = TextView(this).apply {
                text = dia
                setPadding(16, 8, 16, 8)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(250, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundColor(Color.parseColor("#DDDDDD"))
            }
            headerDays.addView(textView)
        }
    }

    private fun agregarHoras() {
        for (hora in 0..23) {
            val formatoHora = String.format("%02d:00", hora)
            val textView = TextView(this).apply {
                text = formatoHora
                setPadding(8, 32, 8, 32)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 120)
                gravity = Gravity.CENTER_VERTICAL
            }
            hourLabels.addView(textView)
        }
    }

    private fun generarCeldasVacias() {
        for (hora in 0..23) {
            for (dia in 0 until diasSemana.size) {
                val celda = TextView(this).apply {
                    layoutParams = ViewGroup.LayoutParams(250, 120)
                    setBackgroundColor(Color.parseColor("#F2F2F2"))
                    setPadding(4, 4, 4, 4)
                    text = ""
                }
                gridHorario.addView(celda)
            }
        }
    }
}