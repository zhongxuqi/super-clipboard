package com.musketeer.superclipboard

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.musketeer.superclipboard.data.ClipBoardMessage
import com.musketeer.superclipboard.db.SqliteHelper


class MainService : Service() {
    var manager: ClipboardManager? = null
    var prevValue: String = ""

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager!!.addPrimaryClipChangedListener {
            if (manager!!.hasPrimaryClip() && manager!!.primaryClip!!.itemCount > 0) {
                val addedText = manager!!.primaryClip!!.getItemAt(0).text
                val millisTs = System.currentTimeMillis()
                val newValue = addedText.toString()
                if (prevValue.compareTo(newValue) != 0) {
                    prevValue = newValue
                    SqliteHelper.helper!!.Insert(ClipBoardMessage(0, ClipBoardMessage.MessageType.Text, newValue, "", millisTs, millisTs))
                    ClipboardMainWindow.refreshAdapter()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
