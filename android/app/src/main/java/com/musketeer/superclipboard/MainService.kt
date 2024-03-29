package com.musketeer.superclipboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.musketeer.superclipboard.data.ClipBoardMessage
import com.musketeer.superclipboard.db.SqliteHelper
import com.musketeer.superclipboard.net.UdpClient


class MainService : Service(), ClipboardManager.OnPrimaryClipChangedListener {
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
        manager.addPrimaryClipChangedListener(this)
    }

    override fun onPrimaryClipChanged() {
        if (manager.hasPrimaryClip() && manager.primaryClip!!.itemCount > 0) {
            if (skipNum > 0) {
                skipNum = 0
                return
            }
            val addedText = manager.primaryClip!!.getItemAt(0).text ?: return
            val millisTs = System.currentTimeMillis()
            val newValue = addedText.toString()
            prevValue = newValue
            val msgObj = ClipBoardMessage(0, ClipBoardMessage.MessageType.Text, newValue, "", 0, 0)
            UdpClient.Instance!!.sendClipboardMsg(msgObj)
            msgObj.createTime = millisTs
            msgObj.updateTime = millisTs
            insertMessage(ClipBoardMessage(0, ClipBoardMessage.MessageType.Text, newValue, "", millisTs, millisTs))
        }
    }

    fun insertMessage(clipboardMessage: ClipBoardMessage) {
        val existMsg = ClipboardMainWindow.Instance?.getSameMessage(clipboardMessage)
        skipNum++
        prevValue = clipboardMessage.content
        manager.setPrimaryClip(ClipData.newPlainText(prevValue, prevValue))
        ClipboardMainWindow.Instance!!.showContent(clipboardMessage.content)
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            notificationManager.createNotificationChannel(NotificationChannel(channelID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT));
            val builder = NotificationCompat.Builder(this, channelID);
            startForeground(msgID, builder.build())
        } else {
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
    }

    class MainServiceBinder: Binder()

    val binder: MainServiceBinder by lazy {
        MainServiceBinder()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.removePrimaryClipChangedListener(this)
    }
}
