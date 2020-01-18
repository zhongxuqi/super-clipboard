package com.musketeer.superclipboard

import android.app.Application
import android.content.Intent

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, MainService::class.java)
        startService(intent)
    }
}