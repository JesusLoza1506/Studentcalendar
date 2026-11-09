package com.example.studentcalendar

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenLayoutTest {
    @Test fun splashAndSignIn() = check(R.layout.activity_splash, R.id.logoImage, R.layout.activity_login, R.id.btnGoogleSignIn)

    @Test fun agendaHomeAndNavigation() = check(R.layout.activity_main, R.id.btnAddTask, R.layout.activity_main, R.id.bottom_navigation)

    @Test fun createAndEditEvents() = check(
        R.layout.activity_anadir_clase_tarea, R.id.btn_save_event,
        R.layout.activity_editar_evento, R.id.btnGuardarCambios,
        R.layout.dialog_color_selector, R.id.colorGrid
    )

    @Test fun weeklyScheduleAndFilter() = check(
        R.layout.activity_horario_semanal, R.id.tvCurrentWeekRange,
        R.layout.dialog_filter_schedule, R.id.btnApplyFilter
    )

    @Test fun searchAndReminderList() = check(
        R.layout.activity_ver_tareas, R.id.rvEventos,
        R.layout.activity_notificaciones, R.id.contenedorRecordatorios
    )

    @Test fun exportAndPreview() = check(
        R.layout.activity_exportar_horario_pdf, R.id.btnGenerarPdf,
        R.layout.activity_exportar_horario_pdf, R.id.ivVistaPrevia
    )

    @Test fun settingsProfileSyncAndTerms() = check(
        R.layout.activity_ajustes, R.id.cardPerfil,
        R.layout.dialog_editar_perfil, R.id.etNuevoNombre,
        R.layout.activity_datos_sincronizados, R.id.tvSyncDataTitle,
        R.layout.activity_terminos_condiciones, R.id.tvBody
    )

    private fun check(vararg ids: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val app = instrumentation.targetContext
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val config = Configuration(app.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val themed = ContextThemeWrapper(app.createConfigurationContext(config), R.style.Theme_StudentCalendar_NoActionBar)
                val inflater = LayoutInflater.from(themed)
                for ((layout, id) in ids.toList().chunked(2)) {
                    val root = inflater.inflate(layout, null)
                    assertNotNull("Missing view ${app.resources.getResourceName(id)} in night mode $night", root.findViewById<View>(id))
                }
            }
        }
    }
}
