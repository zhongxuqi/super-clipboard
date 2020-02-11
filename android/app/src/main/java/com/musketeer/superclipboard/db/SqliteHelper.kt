package com.musketeer.superclipboard.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.musketeer.superclipboard.data.ClipBoardMessage
import java.util.*

class SqliteHelper: SQLiteOpenHelper {
    companion object {
        val MaxSize = 10
        private val DB_VERSION = 1
        private val DB_NAME = "my.db"
        val TABLE_NAME = "clipboard_history"
        var helper: SqliteHelper? = null
    }

    constructor(ctx: Context): super(ctx, DB_NAME, null, DB_VERSION)

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "create table if not exists $TABLE_NAME (id INTEGER PRIMARY KEY AUTOINCREMENT, type INT NOT NULL, content TEXT NOT NULL, extra TEXT NOT NULL, create_time BIGINT NOT NULL, update_time BIGINT NOT NULL)"
        db!!.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun ListAll(): LinkedList<ClipBoardMessage> {
        val msgList = LinkedList<ClipBoardMessage>()
        val db = writableDatabase
        val cursor = db.query(TABLE_NAME, arrayOf("id", "type", "content", "extra", "create_time", "update_time"), null, null, null, null, "create_time DESC")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val type = ClipBoardMessage.ToMessageType(cursor.getInt(cursor.getColumnIndex("type")))
            val content = cursor.getString(cursor.getColumnIndex(("content")))
            val extra = cursor.getString(cursor.getColumnIndex("extra"))
            val createTime = cursor.getLong(cursor.getColumnIndex("create_time"))
            val updateTime = cursor.getLong(cursor.getColumnIndex("update_time"))
            msgList.add(ClipBoardMessage(id, type, content, extra, createTime, updateTime))
        }
        cursor.close()
        while (msgList.size > MaxSize) {
            val last = msgList.last
            Delete(last.id)
            msgList.removeLast()
        }
        return msgList
    }

    fun GetLast(): ClipBoardMessage? {
        val db = writableDatabase
        val cursor = db.query(TABLE_NAME, arrayOf("id", "type", "content", "extra", "create_time", "update_time"), null, null, null, null, "create_time DESC", "1")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val type = ClipBoardMessage.ToMessageType(cursor.getInt(cursor.getColumnIndex("type")))
            val content = cursor.getString(cursor.getColumnIndex(("content")))
            val extra = cursor.getString(cursor.getColumnIndex("extra"))
            val createTime = cursor.getLong(cursor.getColumnIndex("create_time"))
            val updateTime = cursor.getLong(cursor.getColumnIndex("update_time"))
            cursor.close()
            return ClipBoardMessage(id, type, content, extra, createTime, updateTime)
        }
        cursor.close()
        return null
    }

    fun Delete(id: Int) {
        val db = writableDatabase
        db.execSQL("delete from $TABLE_NAME where id = $id")
    }

    fun Insert(msg: ClipBoardMessage) {
        val db = writableDatabase
        db.execSQL("insert into $TABLE_NAME (type,content,extra,create_time,update_time) values (${ClipBoardMessage.FromMessageType(msg.type)},'${msg.content.replace("'","''")}','${msg.extra.replace("'","''")}',${msg.createTime},${msg.updateTime})")
    }

    fun Update(msg: ClipBoardMessage) {
        val db = writableDatabase
        db.execSQL("update $TABLE_NAME set update_time=${msg.updateTime} where id=${msg.id}")
    }
}