package com.musketeer.superclipboard

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log


class MainService : Service() {
    var manager: ClipboardManager? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager!!.addPrimaryClipChangedListener {
            if (manager!!.hasPrimaryClip() && manager!!.primaryClip!!.itemCount > 0) {
                val addedText =
                    manager!!.primaryClip!!.getItemAt(0).text
                System.out.printf("===>>> %s\n", addedText.toString())
                Log.w("clipboard", "get text: $addedText")
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}