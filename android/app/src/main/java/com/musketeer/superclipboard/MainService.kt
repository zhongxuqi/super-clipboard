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
import com.musketeer.superclipboard.net.UdpClient


class MainService : Service() {
    companion object {
        var instance: MainService? = null
    }

    val msgID = 1
    val channelID = "main_service_channel"
    val manager: ClipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    var prevValue: String = ""
    var skipNum: Int = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        notify("")
        val last = SqliteHelper.helper!!.GetLast()
        if (last != null) {
            prevValue = last.content
        }
        manager.addPrimaryClipChangedListener(object: ClipboardManager.OnPrimaryClipChangedListener {
            override fun onPrimaryClipChanged() {
                if (manager.hasPrimaryClip() && manager.primaryClip!!.itemCount > 0) {
                    if (skipNum > 0) {
                        skipNum--
                        return
                    }
                    val addedText = manager.primaryClip!!.getItemAt(0).text
                    val millisTs = System.currentTimeMillis()
                    val newValue = addedText.toString()
                    if (prevValue.compareTo(newValue) != 0) {
                        prevValue = newValue
                        val msgObj = ClipBoardMessage(0, ClipBoardMessage.MessageType.Text, newValue, "", 0, 0)
                        UdpClient.Instance!!.sendClipboardMsg(msgObj)
                        msgObj.createTime = millisTs
                        msgObj.updateTime = millisTs
                        insertMessage(ClipBoardMessage(0, ClipBoardMessage.MessageType.Text, newValue, "", millisTs, millisTs))
                    }
                }
            }
        })
    }

    fun insertMessage(clipboardMessage: ClipBoardMessage) {
        val existMsg = ClipboardMainWindow.Instance?.getSameMessage(clipboardMessage)
        if (existMsg != null) {
            existMsg.updateTime = System.currentTimeMillis()
            SqliteHelper.helper!!.Update(existMsg)
            ClipboardMainWindow.Instance?.topMessage(existMsg)
            return
        }
        SqliteHelper.helper!!.Insert(clipboardMessage)
        ClipboardMainWindow.Instance?.addMessage(SqliteHelper.helper!!.GetLast()!!)
        notify(clipboardMessage.content)
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

    }

    val binder: MainServiceBinder by lazy {
        MainServiceBinder()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }
}
