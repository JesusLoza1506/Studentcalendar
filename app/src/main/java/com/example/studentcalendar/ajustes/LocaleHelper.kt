package com.example.studentcalendar.ajustes

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LocaleHelper {

    private const val SHARED_PREFS_NAME = "Ajustes"
    private const val SELECTED_LANGUAGE_KEY = "idioma"
    private const val DEFAULT_LANGUAGE = "es"

    fun establecerLocalizacion(context: Context): Context {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val idioma = prefs.getString(SELECTED_LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        return actualizarRecursos(context, idioma)
    }

    @SuppressLint("ApplySharedPref")
    fun cambiarIdioma(context: Context, nuevoIdioma: String) {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_LANGUAGE_KEY, nuevoIdioma)
            .commit()
    }

    private fun actualizarRecursos(context: Context, idioma: String): Context {
        val locale: Locale = when (idioma) {
            "pt-rBR" -> Locale("pt", "BR")
            else -> Locale(idioma)
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }
}