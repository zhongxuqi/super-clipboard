package com.musketeer.superclipboard

import android.app.Application
import android.content.Intent
import com.musketeer.superclipboard.db.SqliteHelper

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SqliteHelper.helper = SqliteHelper(this)
        val intent = Intent(this, MainService::class.java)
        startService(intent)
    }
}