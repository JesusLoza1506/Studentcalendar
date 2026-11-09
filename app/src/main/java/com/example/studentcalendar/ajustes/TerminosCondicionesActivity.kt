package com.example.studentcalendar.ajustes

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.R
import com.example.studentcalendar.ajustes.LocaleHelper
import com.example.studentcalendar.databinding.ActivityTerminosCondicionesBinding

open class TerminosCondicionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTerminosCondicionesBinding

    override fun attachBaseContext(newBase: Context) {
        val contextWithLocale = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminosCondicionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTitle.text = getString(R.string.terms_conditions_title)
        binding.tvBody.text = getString(R.string.terms_conditions_body)
    }
}