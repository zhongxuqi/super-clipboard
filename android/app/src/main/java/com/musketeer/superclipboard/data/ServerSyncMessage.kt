package com.musketeer.superclipboard.data

import com.alibaba.fastjson.annotation.JSONField
import com.alibaba.fastjson.serializer.JSONSerializer
import com.alibaba.fastjson.serializer.SerializeWriter
import com.alibaba.fastjson.serializer.SerializerFeature
import com.musketeer.superclipboard.net.HttpClient

class ServerSyncMessage {

    @JSONField(name="app_id")
    val appID = HttpClient.AppID
    @JSONField(name="user_id")
    var userID: String = ""
    @JSONField(name="udp_addrs")
    var udpAddrs = Array<String>(0, {""})

    fun toJSON(): String {
        val out = SerializeWriter()
        try {
            val serializer = JSONSerializer(out)

            for (feature in ClipBoardMessage.Config) {
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