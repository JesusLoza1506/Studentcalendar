package com.example.studentcalendar

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.studentcalendar.ajustes.LocaleHelper

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val contextWithLocale = LocaleHelper.establecerLocalizacion(newBase)
        super.attachBaseContext(contextWithLocale)
    }
}
