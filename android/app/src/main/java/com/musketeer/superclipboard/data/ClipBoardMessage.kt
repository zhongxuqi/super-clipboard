package com.musketeer.superclipboard.data

import com.alibaba.fastjson.annotation.JSONCreator
import com.alibaba.fastjson.serializer.JSONSerializable
import com.alibaba.fastjson.serializer.JSONSerializer
import com.alibaba.fastjson.serializer.SerializeWriter
import com.alibaba.fastjson.serializer.SerializerFeature
import org.json.JSONObject
import java.lang.reflect.Type

class ClipBoardMessage {
    enum class MessageType(val c: Int): JSONSerializable {
        Unknow(0),
        Text(1),
        Image(2);

        private val code = c

        override fun write(serializer: JSONSerializer?, fieldName: Any?, fieldType: Type?) {
            val obj = JSONObject()
            obj.put(fieldName.toString(), c)
            serializer?.write(obj)
        }
    }

    companion object {
        val Config = arrayListOf<SerializerFeature>(
            SerializerFeature.WriteNullBooleanAsFalse,
            SerializerFeature.WriteMapNullValue,
            SerializerFeature.WriteNonStringKeyAsString,
            SerializerFeature.WriteNullListAsEmpty,
            SerializerFeature.WriteNullNumberAsZero,
            SerializerFeature.WriteNullStringAsEmpty)

        fun ToMessageType(t: Int): MessageType {
            when (t) {
                1 -> {
                    return MessageType.Text
                }
                2 -> {
                    return MessageType.Image
                }
            }
            return MessageType.Unknow
        }

        fun FromMessageType(t: MessageType): Int {
            when (t) {
                MessageType.Text -> {
                    return 1
                }
                MessageType.Image -> {
                    return 2
                }
            }
            return 0
        }
    }

    var id: Int = 0
    var type: MessageType = MessageType.Unknow
    var content: String = ""
    var extra: String = ""
    var createTime: Long = 0
    var updateTime: Long = 0

    @JSONCreator
    constructor()

    constructor(id: Int, type: MessageType, content: String, extra: String, createTime: Long, updateTime: Long) {
        this.id = id
        this.type = type
        this.content = content
        this.extra = extra
        this.createTime = createTime
        this.updateTime = updateTime
    }

    fun cloneMessage(): ClipBoardMessage {
        return ClipBoardMessage(this.id, this.type, this.content, this.extra, this.createTime, this.updateTime)
    }

    fun toJSON(): String {
        val out = SerializeWriter()
        try {
            val serializer = JSONSerializer(out)

            for (feature in Config) {
                serializer.config(feature, true)
            }
            serializer.config(SerializerFeature.WriteEnumUsingToString, false)
            serializer.write(this)

            return  out.toString()
        } finally {
            out.close()
        }
    }
}