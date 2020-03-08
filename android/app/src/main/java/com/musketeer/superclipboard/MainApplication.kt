package com.musketeer.superclipboard

import android.app.Application
import android.content.Intent
import android.os.Build
import com.musketeer.superclipboard.db.SqliteHelper
import com.musketeer.superclipboard.net.UdpClient
import com.tencent.tauth.Tencent
import com.umeng.commonsdk.UMConfigure

class MainApplication : Application() {
    val AppChannel = "main"

    override fun onCreate() {
        super.onCreate()
        UMConfigure.init(this, "5e5e6464570df356360001af", AppChannel, UMConfigure.DEVICE_TYPE_PHONE, null)
        SqliteHelper.helper = SqliteHelper(this)
        UdpClient.Instance = UdpClient(this)
        val intent = Intent(this, MainService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}