package com.example.studentcalendar

import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ExportarHorarioPdfActivity : AppCompatActivity() {

    private lateinit var layoutHorarioPreview: LinearLayout
    private lateinit var btnGenerarPDF: MaterialButton
    private lateinit var btnCompartirPDF: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exportar_horario_pdf)
        layoutHorarioPreview = findViewById(R.id.layoutHorarioPreview)
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF)
        btnCompartirPDF = findViewById(R.id.btnCompartirPDF)

        btnGenerarPDF.setOnClickListener {
            generarPDF()
        }

        btnCompartirPDF.setOnClickListener {
            compartirPDF()
        }
    }

    private fun generarPDF() {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            layoutHorarioPreview.width,
            layoutHorarioPreview.height,
            1
        ).create()

        val page = document.startPage(pageInfo)

        layoutHorarioPreview.draw(page.canvas)
        document.finishPage(page)

        val directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(directory, "HorarioSemanal.pdf")

        try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
                Toast.makeText(this, "PDF generado: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
        }
    }

    private fun compartirPDF() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HorarioSemanal.pdf")

        if (file.exists()) {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Compartir horario en PDF"))
        } else {
            Toast.makeText(this, "Primero genera el PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
