package com.musketeer.superclipboard

import android.app.Application
import android.content.Intent
import com.musketeer.superclipboard.db.SqliteHelper
import com.musketeer.superclipboard.net.UdpClient

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SqliteHelper.helper = SqliteHelper(this)
        UdpClient.Instance = UdpClient()
        val intent = Intent(this, MainService::class.java)
        startService(intent)
    }
}