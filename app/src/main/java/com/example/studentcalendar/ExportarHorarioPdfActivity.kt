package com.example.studentcalendar

import android.app.TimePickerDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.example.studentcalendar.eventos.EventSchedule
import com.example.studentcalendar.vertareas.EventConstants
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.studentcalendar.databinding.ActivityExportarHorarioPdfBinding
import com.example.studentcalendar.horariosemanal.CalendarEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.example.studentcalendar.base.BaseActivity


class ExportarHorarioPdfActivity : BaseActivity<ActivityExportarHorarioPdfBinding>() {

    override fun getViewBinding(): ActivityExportarHorarioPdfBinding {
        return ActivityExportarHorarioPdfBinding.inflate(layoutInflater)
    }

    override fun getNavigationMenuItemId(): Int = R.id.nav_export_pdf

    private var semanaActual: Calendar = Calendar.getInstance()
    private var eventosSemana: List<CalendarEvent> = listOf()
    private var weekRequest = 0
    private var pdfReady = false
    private var waitingForDestination = false
    private lateinit var pdfFile: File
    private val savePdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        waitingForDestination = false
        if (uri == null || !pdfFile.isFile) return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                pdfFile.inputStream().use { it.copyTo(output) }
            } ?: error("Could not open PDF destination")
            Toast.makeText(this, R.string.pdf_saved, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_saving_locally, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private var horaInicioFiltro = "08:00"
    private var horaFinFiltro = "18:00"

    private var nombreUsuario: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val folder = File(cacheDir, "schedules").apply { mkdirs() }
        val filename = savedInstanceState?.getString("pendingSaveFilename") ?: "horario_${UUID.randomUUID()}.pdf"
        pdfFile = File(folder, filename)
        establecerTextosLocalizados()
        inicializarSemanaYCargarDatos()
        configurarListeners()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun establecerTextosLocalizados() {
        binding.tvExportTitle.text = getString(R.string.export_title)
        binding.tvExportSubtitle.text = getString(R.string.export_subtitle)
        binding.btnSemanaAnterior.text = getString(R.string.previous_week_button)
        binding.btnSemanaSiguiente.text = getString(R.string.next_week_button)
        binding.checkLunes.text = getString(R.string.day_monday_short)
        binding.checkMartes.text = getString(R.string.day_tuesday_short)
        binding.checkMiercoles.text = getString(R.string.day_wednesday_short)
        binding.checkJueves.text = getString(R.string.day_thursday_short)
        binding.checkViernes.text = getString(R.string.day_friday_short)
        binding.checkSabado.text = getString(R.string.day_saturday_short)
        binding.checkDomingo.text = getString(R.string.day_sunday_short)
        binding.tvHoraInicio.text = getString(R.string.time_range_from, horaInicioFiltro)
        binding.tvHoraFin.text = getString(R.string.time_range_to, horaFinFiltro)
        binding.btnGenerarPdf.text = getString(R.string.button_generate_pdf)
        binding.ivVistaPrevia.contentDescription = getString(R.string.pdf_preview_description)
        binding.btnGuardarLocal.text = getString(R.string.button_save_local)
        binding.btnCompartir.text = getString(R.string.button_share)
    }

    private fun inicializarSemanaYCargarDatos() {
        semanaActual = obtenerInicioSemana(Calendar.getInstance())
        actualizarTextoSemana()
        cargarEventosDeLaSemana()
        cargarNombreUsuario()

        binding.checkLunes.isChecked = true
        binding.checkMartes.isChecked = true
        binding.checkMiercoles.isChecked = true
        binding.checkJueves.isChecked = true
        binding.checkViernes.isChecked = true
    }

    private fun configurarListeners() {
        val diasCheckBoxes = listOf(
            binding.checkLunes, binding.checkMartes, binding.checkMiercoles,
            binding.checkJueves, binding.checkViernes, binding.checkSabado, binding.checkDomingo
        )
        diasCheckBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> invalidatePdf() }
        }

        binding.btnSemanaAnterior.setOnClickListener {
            semanaActual.add(Calendar.WEEK_OF_YEAR, -1)
            actualizarTextoSemana()
            cargarEventosDeLaSemana()
        }

        binding.btnSemanaSiguiente.setOnClickListener {
            semanaActual.add(Calendar.WEEK_OF_YEAR, 1)
            actualizarTextoSemana()
            cargarEventosDeLaSemana()
        }

        binding.tvHoraInicio.setOnClickListener {
            mostrarSelectorTiempo(horaInicioFiltro) { horaSeleccionada ->
                invalidatePdf()
                horaInicioFiltro = horaSeleccionada
                binding.tvHoraInicio.text = getString(R.string.time_range_from, horaSeleccionada)
            }
        }

        binding.tvHoraFin.setOnClickListener {
            mostrarSelectorTiempo(horaFinFiltro) { horaSeleccionada ->
                invalidatePdf()
                horaFinFiltro = horaSeleccionada
                binding.tvHoraFin.text = getString(R.string.time_range_to, horaSeleccionada)
            }
        }

        binding.btnGenerarPdf.setOnClickListener {
            generarPdfConEventos()
        }

        binding.btnGuardarLocal.setOnClickListener {
            if (pdfReady) {
                waitingForDestination = true
                savePdf.launch("Horario-KalendU.pdf")
            }
        }
        binding.btnCompartir.setOnClickListener {
            if (pdfReady) compartirPdf(pdfFile)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (waitingForDestination) outState.putString("pendingSaveFilename", pdfFile.name)
        super.onSaveInstanceState(outState)
    }

    private fun invalidatePdf() {
        pdfReady = false
        binding.ivVistaPrevia.setImageDrawable(null)
        binding.btnGuardarLocal.isEnabled = false
        binding.btnCompartir.isEnabled = false
    }

    private fun cargarNombreUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        nombreUsuario = currentUser?.displayName ?: getString(R.string.pdf_default_user_name)

        if (nombreUsuario == getString(R.string.pdf_default_user_name) && userId.isNotEmpty()) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    nombreUsuario = doc.getString("nombre") ?: getString(R.string.pdf_default_user_name)
                }
                .addOnFailureListener { e ->
                    Log.e("ExportarPDF", "Error al cargar nombre de usuario desde Firestore: ${e.message}")
                }
        }
    }

    private fun actualizarTextoSemana() {
        val inicio = obtenerInicioSemana(semanaActual.clone() as Calendar)
        val fin = (inicio.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val formato = SimpleDateFormat("dd MMM", Locale.getDefault())
        val texto = "${formato.format(inicio.time)} - ${formato.format(fin.time)}"
        binding.tvSemanaSeleccionada.text = texto.uppercase(Locale.getDefault())
    }

    private fun obtenerInicioSemana(fecha: Calendar): Calendar {
        val cal = fecha.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        val offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DATE, -offset)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun cargarEventosDeLaSemana() {
        val request = ++weekRequest
        invalidatePdf()
        binding.btnGenerarPdf.isEnabled = false
        eventosSemana = emptyList()
        if (userId.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_user_not_logged_in_error), Toast.LENGTH_SHORT).show()
            return
        }

        val inicioSemana = obtenerInicioSemana(semanaActual.clone() as Calendar)
        val finSemana = (inicioSemana.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        db.collection("users")
            .document(userId)
            .collection("events")
            .whereGreaterThanOrEqualTo("fecha", inicioSemana.time)
            .whereLessThanOrEqualTo("fecha", finSemana.time)
            .get()
            .addOnSuccessListener { documentos ->
                if (request != weekRequest) return@addOnSuccessListener
                val eventos = documentos.mapNotNull { doc ->
                    try {
                        doc.toObject(CalendarEvent::class.java)?.copy(eventId = doc.id)
                    } catch (e: Exception) {
                        Log.e("ExportarPDF", "Error al parsear documento de evento: ${doc.id}, ${e.message}")
                        null
                    }
                }
                eventosSemana = eventos
                binding.btnGenerarPdf.isEnabled = true
                Toast.makeText(this, getString(R.string.toast_events_loaded, eventosSemana.size), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (request != weekRequest) return@addOnFailureListener
                Toast.makeText(this, getString(R.string.toast_error_loading_events), Toast.LENGTH_LONG).show()
                Log.e("ExportarPDF", "Error al cargar eventos: ${e.message}")
            }
    }

    private fun obtenerDiasSeleccionados(): Set<Int> {
        val dias = mutableSetOf<Int>()
        if (binding.checkLunes.isChecked) dias.add(Calendar.MONDAY)
        if (binding.checkMartes.isChecked) dias.add(Calendar.TUESDAY)
        if (binding.checkMiercoles.isChecked) dias.add(Calendar.WEDNESDAY)
        if (binding.checkJueves.isChecked) dias.add(Calendar.THURSDAY)
        if (binding.checkViernes.isChecked) dias.add(Calendar.FRIDAY)
        if (binding.checkSabado.isChecked) dias.add(Calendar.SATURDAY)
        if (binding.checkDomingo.isChecked) dias.add(Calendar.SUNDAY)
        return dias
    }

    private fun generarPdfConEventos() {
        val from = EventSchedule.minutes(horaInicioFiltro) ?: return
        val to = EventSchedule.minutes(horaFinFiltro) ?: return
        if (from >= to || obtenerDiasSeleccionados().isEmpty()) {
            Toast.makeText(this, R.string.pdf_invalid_filter, Toast.LENGTH_LONG).show()
            return
        }
        invalidatePdf()
        val horaInicio = from / 60
        val minInicio = from % 60
        val horaInicioFloat = from / 60f
        val totalHoras = kotlin.math.ceil((to - from) / 60.0).toInt()

        val pageHeight = (150 + totalHoras * 60 + 100).coerceAtLeast(595)
        val pageInfo = PdfDocument.PageInfo.Builder(842, pageHeight, 1).create()
        val pdfDocument = PdfDocument()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintLinea = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val paintTexto = Paint().apply {
            color = Color.DKGRAY
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintTitulo = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val diasLabels = listOf(
            getString(R.string.day_monday_full),
            getString(R.string.day_tuesday_full),
            getString(R.string.day_wednesday_full),
            getString(R.string.day_thursday_full),
            getString(R.string.day_friday_full),
            getString(R.string.day_saturday_full),
            getString(R.string.day_sunday_full)
        )
        val diasSeleccionados = obtenerDiasSeleccionados()
        val margenIzq = 40f
        val margenDer = 40f
        val anchoTotalPagina = canvas.width.toFloat()
        val anchoContenidoEditable = anchoTotalPagina - margenIzq - margenDer
        val anchoColumnaHoras = 60f
        val anchoDiasTabla = anchoContenidoEditable - anchoColumnaHoras
        val anchoDia = anchoDiasTabla / 7f
        val altoHora = 60f
        var currentY = 50f

        canvas.drawLine(margenIzq, currentY - 20f, anchoTotalPagina - margenDer, currentY - 20f, paintLinea)

        val logoDrawable = ContextCompat.getDrawable(this, R.drawable.logo)
        val logoBitmap: Bitmap? = if (logoDrawable is BitmapDrawable) {
            logoDrawable.bitmap
        } else if (logoDrawable != null) {
            Bitmap.createBitmap(
                logoDrawable.intrinsicWidth.coerceAtLeast(1),
                logoDrawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).also { bitmap ->
                val canvasLogo = Canvas(bitmap)
                logoDrawable.setBounds(0, 0, canvasLogo.width, canvasLogo.height)
                logoDrawable.draw(canvasLogo)
            }
        } else {
            null
        }

        if (logoBitmap != null) {
            val logoWidthPdf = 60f
            val logoHeightPdf = logoBitmap.height * (logoWidthPdf / logoBitmap.width)

            val logoX = margenIzq + 5f
            val titulo = getString(R.string.pdf_title_schedule)
            val textWidthTitulo = paintTitulo.measureText(titulo)

            val espacioDisponibleParaTituloX = anchoTotalPagina - margenDer - (logoX + logoWidthPdf + 15f)
            val tituloX = (logoX + logoWidthPdf + 15f) + (espacioDisponibleParaTituloX / 2) - (textWidthTitulo / 2)

            val maxHeaderContentHeight = maxOf(logoHeightPdf, paintTitulo.textSize)
            val headerContentMidY = currentY + (maxHeaderContentHeight / 2)
            val logoY = headerContentMidY - (logoHeightPdf / 2)
            val tituloY = headerContentMidY + (paintTitulo.textSize / 2) - paintTitulo.descent()

            canvas.drawBitmap(logoBitmap, null, RectF(logoX, logoY, logoX + logoWidthPdf, logoY + logoHeightPdf), null)
            canvas.drawText(titulo, tituloX, tituloY, paintTitulo)

            currentY += maxHeaderContentHeight + 20f
        } else {
            val titulo = getString(R.string.pdf_title_schedule)
            val textWidthTitulo = paintTitulo.measureText(titulo)
            val tituloX = (anchoTotalPagina - textWidthTitulo) / 2f
            canvas.drawText(titulo, tituloX, currentY + paintTitulo.textSize, paintTexto) // Changed paintTitulo to paintTexto
            currentY += paintTitulo.textSize + 20f
            Toast.makeText(this, getString(R.string.toast_logo_not_found), Toast.LENGTH_LONG).show()
        }

        val infoX = margenIzq
        canvas.drawText(getString(R.string.pdf_user_label, nombreUsuario), infoX, currentY, paintTexto)
        currentY += paintTexto.textSize + 10f
        canvas.drawText(getString(R.string.pdf_week_label, binding.tvSemanaSeleccionada.text), infoX, currentY, paintTexto)
        currentY += paintTexto.textSize + 25f

        canvas.drawLine(margenIzq, currentY, anchoTotalPagina - margenDer, currentY, paintLinea)

        val margenTopTabla = currentY + 40f
        val xTablaInicioReal = margenIzq + anchoColumnaHoras

        for ((index, diaNombre) in diasLabels.withIndex()) {
            val x = xTablaInicioReal + index * anchoDia
            val diaTextoWidth = paintTexto.measureText(diaNombre)
            canvas.drawText(diaNombre, x + (anchoDia / 2) - (diaTextoWidth / 2), margenTopTabla - 15f, paintTexto)
            canvas.drawLine(x, margenTopTabla, x, margenTopTabla + totalHoras * altoHora, paintLinea)
        }
        canvas.drawLine(xTablaInicioReal + anchoDiasTabla, margenTopTabla, xTablaInicioReal + anchoDiasTabla, margenTopTabla + totalHoras * altoHora, paintLinea)

        for (h in 0..totalHoras) {
            val y = margenTopTabla + h * altoHora
            val currentTotalMinutes = (horaInicio * 60 + minInicio) + (h * 60)
            val horaMostrar = currentTotalMinutes / 60
            val minutoMostrar = currentTotalMinutes % 60
            val horaTexto = String.format("%02d:%02d", horaMostrar, minutoMostrar)

            val horaTextoWidth = paintTexto.measureText(horaTexto)
            val horaTextX = margenIzq + anchoColumnaHoras - horaTextoWidth - 5f
            canvas.drawText(horaTexto, horaTextX, y + paintTexto.textSize / 2, paintTexto)

            canvas.drawLine(xTablaInicioReal, y, xTablaInicioReal + anchoDiasTabla, y, paintLinea)
        }

        for (evento in eventosSemana) {
            val cal = Calendar.getInstance().apply { time = evento.fecha }
            val diaSemana = cal.get(Calendar.DAY_OF_WEEK)

            if (diasSeleccionados.isEmpty() || diaSemana !in diasSeleccionados) {
                continue
            }

            val columna = when (diaSemana) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> {
                    Log.e("ExportarPDF", "Día de la semana inválido para evento: ${evento.nombre}. Día: $diaSemana")
                    continue
                }
            }

            val start = EventSchedule.minutes(evento.horaInicio) ?: continue
            val end = if (evento.tipo == EventConstants.TYPE_TASK) null else EventSchedule.minutes(evento.horaFin) ?: continue
            if (!EventSchedule.visibleBetween(start, end, from, to)) continue
            val iniFloat = maxOf(start, from) / 60f
            val finFloat = minOf(end ?: (start + 30), to) / 60f

            val eventHorizontalPadding = 3f
            val eventRectLeft = xTablaInicioReal + columna * anchoDia + eventHorizontalPadding
            val eventRectRight = xTablaInicioReal + (columna + 1) * anchoDia - eventHorizontalPadding
            val top = margenTopTabla + ((iniFloat - horaInicioFloat) * altoHora).coerceAtMost(totalHoras * altoHora - 20f)
            val height = ((finFloat - iniFloat) * altoHora).coerceAtLeast(20f)

            val paintFondo = Paint().apply {
                color = try {
                    Color.parseColor(evento.color)
                } catch (e: Exception) {
                    Log.e("ExportarPDF", "Color inválido para evento ${evento.nombre}: ${evento.color}. Usando gris claro por defecto.")
                    Color.LTGRAY
                }
            }
            canvas.drawRect(eventRectLeft, top, eventRectRight, top + height, paintFondo)

            val paintTextoEventoVisible = Paint().apply {
                color = if (esColorOscuro(paintFondo.color)) Color.WHITE else Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val textInternalPadding = 4f
            val textYOffsetVertical = 16f

            // Modificaciones para el texto del evento
            val maxTextWidth = eventRectRight - eventRectLeft - (2 * textInternalPadding)

            // Nombre del Evento
            val nombreCompleto = evento.nombre
            var nombreAjustado = ""
            var currentLine = ""
            val words = nombreCompleto.split(" ")
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paintTextoEventoVisible.measureText(testLine) < maxTextWidth) {
                    currentLine = testLine
                } else {
                    if (nombreAjustado.isNotEmpty()) nombreAjustado += "\n"
                    nombreAjustado += currentLine
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                if (nombreAjustado.isNotEmpty()) nombreAjustado += "\n"
                nombreAjustado += currentLine
            }

            val nombreLines = nombreAjustado.split("\n")
            var lineCount = 0
            for (line in nombreLines) {
                if (height > textYOffsetVertical * (lineCount + 1)) {
                    canvas.drawText(line, eventRectLeft + textInternalPadding, top + textYOffsetVertical * (lineCount + 1), paintTextoEventoVisible)
                    lineCount++
                } else {
                    break
                }
            }

            // Horario del Evento
            val horario = if (end == null) evento.horaInicio else "${evento.horaInicio} - ${evento.horaFin}"
            if (height > textYOffsetVertical * (lineCount + 1)) {
                canvas.drawText(horario, eventRectLeft + textInternalPadding, top + textYOffsetVertical * (lineCount + 1), paintTextoEventoVisible)
                lineCount++
            }

            // Aula del Evento con el prefijo y emoji
            if (height > textYOffsetVertical * (lineCount + 1) && !evento.aula.isNullOrBlank()) {
                val aulaTexto = "📍 Aula: ${evento.aula}"
                // Implementar ajuste de texto para el aula si es necesario, similar al nombre
                val aulaAjustada = if (paintTextoEventoVisible.measureText(aulaTexto) > maxTextWidth) {
                    val charsToFit = paintTextoEventoVisible.breakText(aulaTexto, true, maxTextWidth, null)
                    aulaTexto.substring(0, charsToFit) + "..."
                } else {
                    aulaTexto
                }
                canvas.drawText(aulaAjustada, eventRectLeft + textInternalPadding, top + textYOffsetVertical * (lineCount + 1), paintTextoEventoVisible)
            }
        }

        pdfDocument.finishPage(page)
        try {
            FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
            pdfReady = true
            binding.btnGuardarLocal.isEnabled = true
            binding.btnCompartir.isEnabled = true
            Toast.makeText(this, getString(R.string.toast_pdf_generated), Toast.LENGTH_SHORT).show()
            mostrarVistaPreviaPdf(pdfFile)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_saving_pdf, e.message), Toast.LENGTH_LONG).show()
            Log.e("ExportarPDF", "Error al guardar el PDF: ${e.message}", e)
        } finally {
            pdfDocument.close()
        }
    }

    private fun mostrarVistaPreviaPdf(pdfFile: File) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)

            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)

                val previewWidth = binding.ivVistaPrevia.width
                val previewHeight = binding.ivVistaPrevia.height
                val aspectRatio = page.width.toFloat() / page.height.toFloat()

                val bitmapWidth = if (previewWidth > 0) previewWidth else 842
                val bitmapHeight = (bitmapWidth / aspectRatio).toInt()

                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                binding.ivVistaPrevia.setImageBitmap(bitmap)

                page.close()
            }
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_rendering_preview, e.message), Toast.LENGTH_LONG).show()
            Log.e("ExportarPDF", "Error al renderizar vista previa del PDF: ${e.message}", e)
        }
    }

    private fun esColorOscuro(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun mostrarSelectorTiempo(horaActual: String, callback: (String) -> Unit) {
        val partes = horaActual.split(":")
        val hora = partes[0].toInt()
        val minuto = partes[1].toInt()

        val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val horaFormateada = String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute)
            callback(horaFormateada)
        }, hora, minuto, true)

        timePicker.setTitle(getString(R.string.time_picker_title))
        timePicker.show()
    }

    private fun compartirPdf(pdfFile: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_pdf_chooser_title))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.share_pdf_chooser_title)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_sharing_pdf, e.message), Toast.LENGTH_LONG).show()
            Log.e("ExportarPDF", "Error al compartir PDF: ${e.message}", e)
        }
    }
}
