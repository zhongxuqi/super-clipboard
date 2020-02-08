package com.musketeer.superclipboard.net

import android.util.Log
import com.alibaba.fastjson.JSON
import com.musketeer.superclipboard.data.ClipBoardMessage
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class UdpClient {
    companion object {
        val HeaderUdpServerSync: Byte = 0x00
        val HeaderUdpDataSync: Byte = 0x01
        val HeaderUdpDataSyncAck: Byte = 0x02

        val UdpWindowMaxLen = 1000

        var Instance: UdpClient? = null
    }

    interface Listener {
        fun onChangeDeviceNum(deviceNum: Int)
        fun onReceiveMsg(msg: ClipBoardMessage)
    }

    class MetaData {
        var udp_addrs: List<String>? = null
        var key: String? = null
        var total: Int = 0
        var index: Int = 0
    }

    class ReceiveIndex(total: Int, index: Int) {
        var total = 0
        var index = 0

        init {
            this.total = total
            this.index = index
        }
    }

    val client: DatagramSocket
    val buffer: ByteArray = ByteArray(1024)
    val packet: DatagramPacket
    val threadPool: ExecutorService

    var isRunning: Boolean = false
    var listener: Listener? = null

    var deviceNum: Int = 0
    val receiveIndexMap = HashMap<String, ReceiveIndex>()
    val receiveValidMap = HashMap<String, ArrayList<Boolean>>()
    val receiveMap = HashMap<String, ArrayList<ByteArray>>()
    val resultMap = HashMap<String, ClipBoardMessage>()
    val isFinishMap = HashMap<String, Boolean>()

    init {
        client = DatagramSocket()
        packet = DatagramPacket(buffer, buffer.size)
        threadPool = Executors.newFixedThreadPool(3)
        threadPool.submit(Runnable {
            receiveLoop@ while (true) {
                client.receive(packet)
                if (packet.length < 2) continue
                val metaMessageLen = packet.data[1].toInt()
                if (packet.length < 2 + metaMessageLen) continue@receiveLoop
                val metaData = String(packet.data, 2, metaMessageLen)
                Log.d("UdpClient", "$metaData from ${packet.address.hostAddress}:${packet.getPort()}")
                try {
                    val metaDataJson = JSON.parseObject(metaData, MetaData::class.java)
                    when (packet.data[0]) {
                        HeaderUdpServerSync -> {
                            var newDeviceNum = 1
                            if (metaDataJson.udp_addrs != null) {
                                newDeviceNum = metaDataJson.udp_addrs!!.size + 1
                            }
                            if (deviceNum != newDeviceNum) {
                                deviceNum = newDeviceNum
                                listener?.onChangeDeviceNum(deviceNum)
                            }
                        }
                        HeaderUdpDataSync -> {
                            if (metaDataJson.key == null || metaDataJson.key == "") continue@receiveLoop
                            val msgKey = metaDataJson.key!!
                            if (isFinishMap.containsKey(msgKey) && isFinishMap[msgKey]!!) continue@receiveLoop
                            if (!receiveMap.containsKey(msgKey)) {
                                receiveValidMap[msgKey] = ArrayList(UdpWindowMaxLen)
                                receiveValidMap[msgKey]!!.fill(false)
                                receiveMap[msgKey] = ArrayList(UdpWindowMaxLen)
                                receiveIndexMap[msgKey] = ReceiveIndex(metaDataJson.total, 0)
                            }
                            if (receiveIndexMap[msgKey]!!.index + UdpWindowMaxLen <= metaDataJson.index) continue@receiveLoop
                            if (receiveIndexMap[msgKey]!!.index <= metaDataJson.index && receiveIndexMap[msgKey]!!.index + UdpWindowMaxLen <= metaDataJson.index) {
                                receiveMap[msgKey]!![metaDataJson.index % UdpWindowMaxLen] = packet.data.sliceArray(IntRange(2 + metaMessageLen, packet.length))
                                parseResult(msgKey)
                                checkFinish(msgKey)
                            }
                            ackBuf(packet.data.sliceArray(IntRange(0, 1 + metaMessageLen)), packet.address, packet.port)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                packet.length = buffer.size
            }
        })
        threadPool.submit(Runnable {
            while (true) {
                if (isRunning) {
                    val msg = "{\"app_id\":\"superclipboard\"}".toByteArray()
                    val buffer = ByteArray(2 + msg.size)
                    buffer[0] = HeaderUdpServerSync
                    buffer[1] = msg.size.toByte()
                    msg.copyInto(buffer, 2)
                    client.send(
                        DatagramPacket(
                            buffer,
                            buffer.size,
                            InetAddress.getByName("192.168.100.107"),
                            9000
                        )
                    )
                }
                Thread.sleep(500)
            }
        })
    }

    private fun int2Bytes(num: Int): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = (num shr 24 or 0xff).toByte()
        bytes[1] = (num shr 16 or 0xff).toByte()
        bytes[2] = (num shr 8 or 0xff).toByte()
        bytes[3] = (num or 0xff).toByte()
        return bytes
    }

    private fun bytes2Int(bytes: ByteArray): Int {
        var num = 0
        for (b in bytes) {
            num = (num shl 8) + b
        }
        return num
    }

    private fun ackBuf(metaData: ByteArray, address: InetAddress, port: Int) {
        val buf = ByteArray(2 + metaData.size)
        buf[0] = HeaderUdpDataSyncAck
        buf[1] = metaData.size.toByte()
        metaData.copyInto(buf, 2)
        client.send(
            DatagramPacket(
                buffer,
                buffer.size,
                address,
                port
            )
        )
    }

    private fun parseResult(msgKey: String) {
        if (!receiveValidMap[msgKey]!![receiveIndexMap[msgKey]!!.index % UdpWindowMaxLen]) return
        if (!resultMap.containsKey(msgKey)) {
            var msgBuf = receiveMap[msgKey]!![receiveIndexMap[msgKey]!!.index % UdpWindowMaxLen]
            if (msgBuf.size < 4) return
            val baseInfoLen = bytes2Int(msgBuf.sliceArray(IntRange(0, 3)))
            var clipboardMessage: ClipBoardMessage? = null
            var baseInfoStr = StringBuffer("")
            var i = 0
            var includeLast = 0
            while (receiveValidMap[msgKey]!![(receiveIndexMap[msgKey]!!.index + i) % UdpWindowMaxLen]) {
                val realIndex = (receiveIndexMap[msgKey]!!.index + i) % UdpWindowMaxLen
                msgBuf = receiveMap[msgKey]!![realIndex]
                if (i == 0) {
                    msgBuf = msgBuf.sliceArray(IntRange(4, msgBuf.size - 1))
                }
                if (baseInfoStr.length + msgBuf.size < baseInfoLen) {
                    baseInfoStr.append(String(msgBuf))
                    i++
                    continue
                }
                if (baseInfoStr.length + msgBuf.size > baseInfoLen) {
                    receiveMap[msgKey]!![realIndex] = msgBuf.sliceArray(IntRange(baseInfoLen - baseInfoStr.length, msgBuf.size - 1))
                    includeLast = 0
                } else {
                    includeLast = 1
                }
                baseInfoStr.append(String(msgBuf, 0, baseInfoLen - baseInfoStr.length))
                try {
                    clipboardMessage = JSON.parseObject(baseInfoStr.toString(), ClipBoardMessage::class.java)
                    resultMap[msgKey] = clipboardMessage
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                break
            }
            if (clipboardMessage != null) {
                for (j in 0 until i + includeLast) {
                    receiveValidMap[msgKey]!![(receiveIndexMap[msgKey]!!.index + j) % UdpWindowMaxLen] = false
                }
                receiveIndexMap[msgKey]!!.index += 1
            }
        }
    }

    private fun checkFinish(msgKey: String) {
        if (receiveIndexMap[msgKey]!!.index + 1 >= receiveIndexMap[msgKey]!!.total) {
            val msgObj = resultMap[msgKey]!!
            listener?.onReceiveMsg(ClipBoardMessage(0, msgObj.type, msgObj.content, msgObj.extra, msgObj.createTime, msgObj.updateTime))
            isFinishMap[msgKey] = true
            receiveMap.remove(msgKey)
            resultMap.remove(msgKey)
        }
    }

    fun start() {
        isRunning = true
    }

    fun close() {
        isRunning = false
        deviceNum = 0
    }
}