package com.musketeer.superclipboard

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.musketeer.superclipboard.data.ClipBoardMessage
import com.musketeer.superclipboard.db.SqliteHelper


class MainService : Service() {
    val msgID = 1
    val channelID = "main_service_channel"
    val manager: ClipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    var prevValue: String = ""

    override fun onCreate() {
        super.onCreate()
        notify("")
        manager.addPrimaryClipChangedListener {
            if (manager.hasPrimaryClip() && manager.primaryClip!!.itemCount > 0) {
                val addedText = manager.primaryClip!!.getItemAt(0).text
                val millisTs = System.currentTimeMillis()
                val newValue = addedText.toString()
                if (prevValue.compareTo(newValue) != 0) {
                    prevValue = newValue
                    SqliteHelper.helper!!.Insert(ClipBoardMessage(0, ClipBoardMessage.MessageType.Text, newValue, "", millisTs, millisTs))
                    ClipboardMainWindow.refreshAdapter()
                    notify(newValue)
                }
            }
        }
    }

    fun notify(txt: String) {
        val builder = NotificationCompat.Builder(this, channelID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        if (txt.isEmpty()) {
            builder.setContentText(getText(R.string.clipboard_empty))
        } else {
            builder.setContentText(txt)
        }
        builder.setOngoing(true)

        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(this, 0, intent, 0)
        builder.setContentIntent(pIntent)

        notificationManager.notify(msgID, builder.build())
    }

    class MainServiceBinder: Binder() {
        fun notify(txt: String) {
            MainService@this.notify(txt)
        }
    }

    val binder: MainServiceBinder by lazy {
        MainServiceBinder()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}
