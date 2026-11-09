package com.example.studentcalendar.base

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.studentcalendar.R
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.ajustes.ThemeHelper
import com.example.studentcalendar.inicio.MainActivity
import com.example.studentcalendar.vertareas.VerTareasActivity
import com.example.studentcalendar.horariosemanal.HorarioSemanalActivity
import com.example.studentcalendar.notificaciones.NotificacionesActivity
import com.example.studentcalendar.ExportarHorarioPdfActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: T

    abstract fun getViewBinding(): T

    abstract fun getNavigationMenuItemId(): Int

    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.aplicarTema(this)
        super.onCreate(savedInstanceState)

        binding = getViewBinding()
        setContentView(binding.root)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigation?.setOnItemSelectedListener { item ->
            val isConnected = isNetworkAvailable()


            if (this is MainActivity && !isConnected && item.itemId != R.id.nav_home) {
                showSnackbar(binding.root, "Necesita conexión a Internet para acceder a esta sección.")
                return@setOnItemSelectedListener false // Bloquea la navegación
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    true
                }
                R.id.nav_view_schedule -> {
                    if (this !is HorarioSemanalActivity) {
                        startActivity(Intent(this, HorarioSemanalActivity::class.java))
                    }
                    true
                }
                R.id.nav_view_tasks -> {
                    if (this !is VerTareasActivity) {
                        startActivity(Intent(this, VerTareasActivity::class.java))
                    }
                    true
                }
                R.id.nav_notifications -> {
                    if (this !is NotificacionesActivity) {
                        startActivity(Intent(this, NotificacionesActivity::class.java))
                    }
                    true
                }
                R.id.nav_export_pdf -> {
                    if (this !is ExportarHorarioPdfActivity) {
                        startActivity(Intent(this, ExportarHorarioPdfActivity::class.java))
                    }
                    true
                }
                else -> false
            }
        }

        adjustUIBasedOnNetwork()
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.selectedItemId = getNavigationMenuItemId()

        adjustUIBasedOnNetwork()
    }

    /**
     * Verifica si hay conexión a Internet activa.
     * @return true si hay conexión, false en caso contrario.
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    /**
     * Muestra un Snackbar en la vista proporcionada.
     * @param view La vista de la cual se 'anclará' el Snackbar (ej. binding.root).
     * @param message El mensaje a mostrar.
     * @param duration La duración del Snackbar (Snackbar.LENGTH_SHORT, Snackbar.LENGTH_LONG, Snackbar.LENGTH_INDEFINITE).
     */
    fun showSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(view, message, duration).show()
    }

    /**
     * Habilita o deshabilita una lista de vistas (botones).
     * @param views Una lista de vistas a gestionar.
     * @param enable true para habilitar, false para deshabilitar.
     */
    fun toggleViewsEnabled(views: List<View>, enable: Boolean) {
        views.forEach { it.isEnabled = enable }
    }

    /**
     * Habilita o deshabilita los ítems de un BottomNavigationView,
     * permitiendo que un ítem específico (ej. Home) siempre esté habilitado si se deshabilita el resto.
     * @param bottomNavigationView La instancia de BottomNavigationView.
     * @param enable true para habilitar todos los ítems, false para deshabilitar la mayoría.
     * @param exceptionItemId El ID del ítem que debe permanecer habilitado (ej. R.id.nav_home) cuando los demás se deshabilitan.
     */
    fun toggleBottomNavItemsEnabled(bottomNavigationView: BottomNavigationView, enable: Boolean, exceptionItemId: Int) {
        val menu = bottomNavigationView.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            menuItem.isEnabled = enable || (menuItem.itemId == exceptionItemId)
        }
    }

    /**
     * Función interna para ajustar la UI según el estado de la red.
     * Se puede sobrescribir en subclases para comportamientos específicos.
     */
    protected open fun adjustUIBasedOnNetwork() {
        val isConnected = isNetworkAvailable()
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (bottomNavigation != null) {
            toggleBottomNavItemsEnabled(bottomNavigation, isConnected, R.id.nav_home)
        }

        if (this is MainActivity && !isConnected) {
            showSnackbar(binding.root, "Necesita conexión a Internet, conéctese a una red.", Snackbar.LENGTH_LONG)
        }
    }
}