package com.example.studentcalendar.inicio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studentcalendar.R
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.ajustes.ThemeHelper
import com.example.studentcalendar.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.establecerLocalizacion(this)
        ThemeHelper.aplicarTema(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this)
            .asGif()
            .load(R.drawable.xd)
            .into(binding.logoImage)

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            verificarUsuarioActual()
        }, 2000)
    }

    private fun verificarUsuarioActual() {
        val user = auth.currentUser
        if (user != null) {
            Log.d("SplashActivity", "Usuario detectado: ${user.uid}")
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            Log.d("SplashActivity", "No se detectó sesión. Redirigiendo a Login.")
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}