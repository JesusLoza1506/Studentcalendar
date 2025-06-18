package com.example.studentcalendar

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.studentcalendar.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class PantallaDeCargaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar el GIF con Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.xd) // Asegúrate de que sea un GIF
            .into(binding.logoImage)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Esperar 2 segundos y decidir a dónde ir
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                // Ya está logueado
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // No está logueado
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2000)
    }
}
