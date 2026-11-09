package com.example.studentcalendar.ajustes

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    private const val PREFS_NAME = "AppThemePrefs"
    private const val KEY_DARK_MODE = "dark_mode"

    fun estaModoOscuroActivado(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun establecerModoOscuro(context: Context, activado: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, activado).apply()

        AppCompatDelegate.setDefaultNightMode(
            if (activado) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun aplicarTema(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (estaModoOscuroActivado(context)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}