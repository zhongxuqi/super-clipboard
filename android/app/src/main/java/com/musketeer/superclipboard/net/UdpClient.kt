package com.musketeer.superclipboard.net

import android.util.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.annotation.JSONField
import com.musketeer.superclipboard.data.ClipBoardMessage
import com.musketeer.superclipboard.data.ServerSyncMessage
import java.net.*
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.experimental.and


class UdpClient {
    companion object {
        val UdpServerHost = "www.easypass.tech"
//        val UdpServerHost = "192.168.100.107"

        val HeaderUdpServerSync: Byte = 0x00
        val HeaderUdpClientSync: Byte = 0x01
        val HeaderUdpDataSync: Byte = 0x02
        val HeaderUdpDataSyncAck: Byte = 0x03

        val UdpAddrSeparator = ","
        val UdpWindowMaxLen = 1000
        val SendBufferMaxLen = 400
        val SyncWorkerTimeout = 3000

        var Instance: UdpClient? = null
        var listener: Listener? = null

        fun int2Bytes(num: Int): ByteArray {
            val bytes = ByteArray(4)
            bytes[0] = (num shr 24 and 0xff).toByte()
            bytes[1] = (num shr 16 and 0xff).toByte()
            bytes[2] = (num shr 8 and 0xff).toByte()
            bytes[3] = (num and 0xff).toByte()
            return bytes
        }

        fun bytes2Int(bytes: ByteArray): Int {
            var num = 0
            for (b in bytes) {
                num = (num shl 8) + b + if (b < 0) 256 else 0
            }
            return num
        }
    }

    interface Listener {
        fun onChangeDeviceNum(isRunning: Boolean, deviceNum: Int)
        fun onReceiveMsg(msg: ClipBoardMessage)
    }

    class MetaData {
        @JSONField(name="udp_addrs")
        var udpAddrs: Array<String>? = null
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

    var localUdpAddrsJoin: String = ""
    val client: DatagramSocket
    val buffer: ByteArray = ByteArray(1024)
    val packet: DatagramPacket
    val threadPool: ScheduledExecutorService

    var isRunning: Boolean = false

    var deviceNum: Int = 0
    val receiveIndexMap = HashMap<String, ReceiveIndex>()
    val receiveMap = HashMap<String, Array<ByteArray?>>()
    val resultMap = HashMap<String, ClipBoardMessage>()
    val isFinishMap = HashMap<String, Boolean>()
    val syncWorkerMap = HashMap<String, SyncWorker>()

    fun SendBuffer(p: DatagramPacket) {
        synchronized(this) {
            try {
                client.send(p)
            } catch (e: java.lang.Exception) {
                Log.e("error", e.toString())
            }
        }
    }

    class ClearKeyTask(val udpClient: UdpClient, val msgKey: String): Runnable {
        override fun run() {
            synchronized(udpClient.isFinishMap) {
                udpClient.isFinishMap.remove(msgKey)
                udpClient.receiveMap.remove(msgKey)
                udpClient.receiveIndexMap.remove(msgKey)
                udpClient.resultMap.remove(msgKey)
            }
        }
    }

    class SyncWorker(val udpClient: UdpClient, val remoteAddr: String) {
        class RemoteAddr(val address: InetAddress, val port: Int)
        private val localUdpClientSyncKey = System.currentTimeMillis().toString()
        private val remoteAddrs: Array<RemoteAddr>
        private var avtiveRemoteAddr: RemoteAddr? = null
        private var isRun = true
        private var lastSyncTime = 0L

        class SendMsg(originMsg: ClipBoardMessage, baseInfoBuffer: ByteArray, key: String, total: Int) {
            val originMsg: ClipBoardMessage = originMsg
            val baseInfoBuffer: ByteArray = baseInfoBuffer
            val key = key
            val total = total
            var index = 0
        }

        private val sendAcks = Array<Boolean>(UdpWindowMaxLen, {false})
        private val sendTimes = Array<Long>(UdpWindowMaxLen, {0L})
        private val sendRetryTime = 3000
        private val sendBuffers = Array<ByteArray?>(UdpWindowMaxLen, {null})
        private val clipboardMsgs = LinkedList<ClipBoardMessage>()
        private var currMsg: SendMsg? = null

        fun toHex(byteArray: ByteArray): String {
            return with(StringBuilder()) {
                byteArray.forEach {
                    val hex = it.toInt() and (0xFF)
                    val hexStr = Integer.toHexString(hex)
                    if (hexStr.length == 1) {
                        this.append("0").append(hexStr)
                    } else {
                        this.append(hexStr)
                    }
                }
                this.toString()
            }
        }

        fun sha256(str:String): String {
            val digest = MessageDigest.getInstance("MD5")
            val result = digest.digest(str.toByteArray())
            return toHex(result)
        }

        fun sendData() {
            if (lastSyncTime + SyncWorkerTimeout < System.currentTimeMillis() || (currMsg == null && clipboardMsgs.size <= 0)) {
                val metaData = "{\"key\":\"$localUdpClientSyncKey\"}".toByteArray()
                val buffer = ByteArray(2 + metaData.size)
                buffer[0] = HeaderUdpClientSync
                buffer[1] = metaData.size.toByte()
                metaData.copyInto(buffer, 2)
                for (remoteAddrItem in remoteAddrs) {
                    udpClient.SendBuffer(
                        DatagramPacket(
                            buffer,
                            buffer.size,
                            remoteAddrItem.address,
                            remoteAddrItem.port
                        )
                    )
//                    Log.d("===>>> client sync", "${remoteAddrItem.address.hostAddress} ${remoteAddrItem.port} $lastSyncTime")
                }
                return
            } else if (currMsg == null && clipboardMsgs.size > 0) {
                var originMsg: ClipBoardMessage? = null
                synchronized(clipboardMsgs) {
                    originMsg = clipboardMsgs.pop()
                }
                originMsg!!.createTime = 0
                originMsg!!.updateTime = 0
                val baseInfoStr = originMsg!!.toJSON()
                val baseInfoByteArray = baseInfoStr.toByteArray()
                currMsg = SendMsg(
                    originMsg!!, baseInfoByteArray, sha256(baseInfoStr),
                    kotlin.math.ceil(baseInfoByteArray.size.toDouble() / SendBufferMaxLen.toDouble()).toInt()
                )
            }
            var i = 0
            while (i < UdpWindowMaxLen && sendBuffers[(i + currMsg!!.index) % UdpWindowMaxLen] != null) {
                val realIndex = (i + currMsg!!.index) % UdpWindowMaxLen
                val buffer = sendBuffers[realIndex]
                if (!sendAcks[realIndex] && sendTimes[realIndex] + sendRetryTime < System.currentTimeMillis() && buffer != null) {
                    udpClient.SendBuffer(
                        DatagramPacket(
                            buffer,
                            buffer.size,
                            avtiveRemoteAddr!!.address,
                            avtiveRemoteAddr!!.port
                        )
                    )
                    sendTimes[realIndex] = System.currentTimeMillis()
                }
                i++
            }
            while ((i + currMsg!!.index) < UdpWindowMaxLen && (i + currMsg!!.index) < currMsg!!.total) {
                if ((i + currMsg!!.index) * SendBufferMaxLen < currMsg!!.baseInfoBuffer.size) {
                    val realIndex = (i + currMsg!!.index) % UdpWindowMaxLen
                    val metaDataJson = MetaData()
                    metaDataJson.key = currMsg!!.key
                    metaDataJson.total = currMsg!!.total
                    metaDataJson.index = currMsg!!.index + i
                    val metaData = JSON.toJSONString(metaDataJson).toByteArray()
                    var bufferLen = 0
                    var isFirst = 0
                    if (metaDataJson.index == 0) isFirst = 4
                    if (currMsg!!.baseInfoBuffer.size > (metaDataJson.index + 1) * SendBufferMaxLen) {
                        bufferLen = SendBufferMaxLen
                    } else {
                        bufferLen = currMsg!!.baseInfoBuffer.size - metaDataJson.index * SendBufferMaxLen
                    }
                    val buffer = ByteArray(2 + metaData.size + isFirst + bufferLen)
                    buffer[0] = HeaderUdpDataSync
                    buffer[1] = metaData.size.toByte()
                    metaData.copyInto(buffer, 2)
                    if (isFirst > 0) {
                        val lenBytes = int2Bytes(currMsg!!.baseInfoBuffer.size)
                        lenBytes.copyInto(buffer, 2 + metaData.size)
//                        Log.d("===>>>", "baseInfoBuffer.size ${currMsg!!.baseInfoBuffer.size}")
                    }
                    currMsg!!.baseInfoBuffer.copyInto(buffer, 2 + metaData.size + isFirst, metaDataJson.index * SendBufferMaxLen, metaDataJson.index * SendBufferMaxLen + bufferLen)
                    sendBuffers[realIndex] = buffer
                    sendTimes[realIndex] = System.currentTimeMillis()
//                    Log.d("===>>> send", "${buffer.size} ${metaDataJson.index} ${String(currMsg!!.baseInfoBuffer.sliceArray(IntRange(metaDataJson.index * SendBufferMaxLen, metaDataJson.index * SendBufferMaxLen + 10)))}")
                    udpClient.SendBuffer(
                        DatagramPacket(
                            buffer,
                            buffer.size,
                            avtiveRemoteAddr!!.address,
                            avtiveRemoteAddr!!.port
                        )
                    )
                }
                i++
            }
        }

        init {
            val remoteAddrs = ArrayList<RemoteAddr>()
            for (remoteAddrItem in remoteAddr.split(UdpAddrSeparator)) {
                val addrItems = remoteAddrItem.split(":")
                if (addrItems.size < 2) continue
                remoteAddrs.add(RemoteAddr(InetAddress.getByName(addrItems[0]), addrItems[1].toInt()))
            }
            this.remoteAddrs = remoteAddrs.toTypedArray()
            sendData()
            Thread(object: Runnable{
                override fun run() {
                    while (isRun) {
                        Thread.sleep(500)
                        if (!isRun) return
                        sendData()
                    }
                }
            }).start()
        }

        fun isActive(): Boolean {
            return isRun && lastSyncTime + SyncWorkerTimeout > System.currentTimeMillis()
        }

        fun close() {
//            Log.d("===>>>", "close ${address.hostAddress}:$port")
            isRun = false
        }

        fun sendClipboardMsg(msgObj: ClipBoardMessage) {
            synchronized(clipboardMsgs) {
                clipboardMsgs.add(msgObj)
            }
        }

        fun feed(metaData: MetaData, address: InetAddress, port: Int) {
            val metaDataKey = metaData.key
//            if (metaDataKey == null || metaDataKey != localUdpClientSyncKey) return
            if (remoteAddr.indexOf("${address.hostAddress}:${port}") < 0) return
            avtiveRemoteAddr = RemoteAddr(address, port)
            lastSyncTime = System.currentTimeMillis()
        }

        fun ack(metaDataJson: MetaData, address: InetAddress, port: Int) {
            synchronized(this) {
                lastSyncTime = System.currentTimeMillis()
                lastSyncTime = System.currentTimeMillis()
                if (currMsg == null || metaDataJson.key != currMsg!!.key) return
                if (metaDataJson.index >= currMsg!!.index && metaDataJson.index < currMsg!!.index + UdpWindowMaxLen) {
                    sendAcks[metaDataJson.index % UdpWindowMaxLen] = true
                }

                // check acks
                var i = 0
                while (sendAcks[(i + currMsg!!.index) % UdpWindowMaxLen]) {
                    val realIndex = (i + currMsg!!.index) % UdpWindowMaxLen
                    sendAcks[realIndex] = false
                    sendTimes[realIndex] = 0
                    sendBuffers[realIndex] = null
                    currMsg!!.index++
                    i++
                }
                if (currMsg!!.index >= currMsg!!.total) {
                    currMsg = null
                    sendAcks.fill(false)
                    sendBuffers.fill(null)
                    sendTimes.fill(0)
                }
            }
        }
    }

    init {
        client = DatagramSocket()
        packet = DatagramPacket(buffer, buffer.size)
        threadPool = Executors.newScheduledThreadPool(1)
        threadPool.submit(Runnable {
            receiveLoop@ while (true) {
                try {
                    client.receive(packet)
                    if (packet.length < 2) continue
                    val metaMessageLen = packet.data[1].toInt()
                    if (packet.length < 2 + metaMessageLen) continue@receiveLoop
                    val metaData = String(packet.data, 2, metaMessageLen)
//                  Log.d("UdpClient", "$metaData from ${packet.address.hostAddress}:${packet.getPort()}")

                    val metaDataJson = JSON.parseObject(metaData, MetaData::class.java)
                    when (packet.data[0]) {
                        HeaderUdpServerSync -> {
                            val validUdpAddrs = ArrayList<String>(0)
                            val udpAddrs = metaDataJson.udpAddrs
                            if (udpAddrs != null) {
                                for (udpAddr in udpAddrs) {
                                    if (localUdpAddrsJoin.equals(udpAddr)) continue
                                    validUdpAddrs.add(udpAddr)
                                }
                            }
                            var deviceNum = 0
                            for (udpAddr in validUdpAddrs) {
                                if (!syncWorkerMap.containsKey(udpAddr)) continue
                                val worker = syncWorkerMap[udpAddr]!!
                                if (worker.isActive()) {
                                    deviceNum++
                                }
                            }
                            listener?.onChangeDeviceNum(isRunning, deviceNum)
                            refreshSyncWork(validUdpAddrs.toTypedArray())
                        }
                        HeaderUdpClientSync -> {
//                            Log.d("UdpClient", "$metaData")
                            val remoteAddr = "${packet.address.hostAddress}:${packet.port}"
                            for (udpAddr in syncWorkerMap.keys) {
                                if (udpAddr.indexOf(remoteAddr) < 0) continue
                                syncWorkerMap[udpAddr]?.feed(metaDataJson, packet.address, packet.port)
                            }
                        }
                        HeaderUdpDataSync -> {
//                            Log.d("UdpClient", "$metaData from ${String(packet.data, 0, packet.length)}")
                            if (metaDataJson.key == null || metaDataJson.key == "") continue@receiveLoop
                            val msgKey = metaDataJson.key!!
                            var isFinished = false
                            synchronized(isFinishMap) {
                                if (isFinishMap.containsKey(msgKey) && isFinishMap[msgKey]!!) {
                                    ackBuf(metaData.toByteArray(), packet.address, packet.port)
                                    if (metaDataJson.index == 0 && resultMap.containsKey(msgKey)) listener?.onReceiveMsg(resultMap[msgKey]!!)
                                    isFinished = true
                                }
                            }
                            if (isFinished) continue@receiveLoop
                            if (!receiveMap.containsKey(msgKey)) {
                                receiveMap[msgKey] = Array<ByteArray?>(UdpWindowMaxLen, {null})
                                receiveIndexMap[msgKey] = ReceiveIndex(metaDataJson.total, 0)
                            }
                            if (receiveIndexMap[msgKey]!!.index + UdpWindowMaxLen <= metaDataJson.index) continue@receiveLoop
                            if (receiveIndexMap[msgKey]!!.index <= metaDataJson.index && receiveIndexMap[msgKey]!!.index + UdpWindowMaxLen > metaDataJson.index) {
                                receiveMap[msgKey]!![metaDataJson.index % UdpWindowMaxLen] = packet.data.sliceArray(IntRange(2 + metaMessageLen, packet.length - 1))
                                parseResult(msgKey)
                                checkFinish(msgKey)
                            }
                            ackBuf(metaData.toByteArray(), packet.address, packet.port)
                        }
                        HeaderUdpDataSyncAck -> {
//                            Log.d("===>>> ack", metaData)
                            val remoteAddr = "${packet.address.hostAddress}:${packet.port}"
                            for (udpAddr in syncWorkerMap.keys) {
                                if (udpAddr.indexOf(remoteAddr) < 0) continue
                                syncWorkerMap[udpAddr]?.ack(metaDataJson, packet.address, packet.port)
                            }
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
                    try {
                        heartBeat()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Thread.sleep(2000)
            }
        })
    }

    private fun heartBeat() {
        try {
            val localUdpAddrMap = HashSet<String>()
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress !is Inet4Address) continue
                    if (!inetAddress.isLoopbackAddress && !inetAddress.isLinkLocalAddress) {
                        localUdpAddrMap.add("${inetAddress.hostAddress}:${client.localPort}")
                    }
                }
            }
            val localUdpAddrs = localUdpAddrMap.toTypedArray().sortedArray()
            localUdpAddrsJoin = localUdpAddrs.joinToString(UdpAddrSeparator)
        } catch (e: Exception) {
            Log.e("error", e.toString())
        }
        val msg = ServerSyncMessage()
        if (localUdpAddrsJoin.isNotEmpty()) msg.udpAddrs = arrayOf(localUdpAddrsJoin)
        val msgByte = msg.toJSON().toByteArray()
        val buffer = ByteArray(2 + msgByte.size)
        buffer[0] = HeaderUdpServerSync
        buffer[1] = msgByte.size.toByte()
        msgByte.copyInto(buffer, 2)
        SendBuffer(
            DatagramPacket(
                buffer,
                buffer.size,
                InetAddress.getByName(UdpServerHost),
                9000
            )
        )
    }

    private fun refreshSyncWork(remoteAddrs: Array<String>?) {
        synchronized(syncWorkerMap) {
            val remoteAddrMap = HashMap<String, Boolean>()
            if (remoteAddrs != null) {
                for (remoteAddr in remoteAddrs) {
                    remoteAddrMap[remoteAddr] = true
                }
            }

            // 删除无效SyncWorker
            val addr2Remove = ArrayList<String>(remoteAddrs?.size ?: 0)
            for (addr in syncWorkerMap.keys) {
                if (!remoteAddrMap.containsKey(addr)) {
                    syncWorkerMap[addr]!!.close()
                    addr2Remove.add(addr)
                }
            }
            for (addr in addr2Remove) {
                syncWorkerMap.remove(addr)
            }

            // 创建新的
            for (addr in remoteAddrMap.keys) {
                if (!syncWorkerMap.containsKey(addr)) {
                    syncWorkerMap[addr] = SyncWorker(this, addr)
                }
            }
        }
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = (bytes[j] and 0xFF.toByte()).toInt()
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun ackBuf(metaData: ByteArray, address: InetAddress, port: Int) {
        val buf = ByteArray(2 + metaData.size)
        buf[0] = HeaderUdpDataSyncAck
        buf[1] = metaData.size.toByte()
        metaData.copyInto(buf, 2)
        SendBuffer(
            DatagramPacket(
                buf,
                buf.size,
                address,
                port
            )
        )
    }

    private fun parseResult(msgKey: String) {
        if (receiveMap[msgKey]!![receiveIndexMap[msgKey]!!.index % UdpWindowMaxLen] == null) return
        if (!resultMap.containsKey(msgKey)) {
            var msgBuf = receiveMap[msgKey]!![receiveIndexMap[msgKey]!!.index % UdpWindowMaxLen]!!
            if (msgBuf.size < 4) return
            val baseInfoLen = bytes2Int(msgBuf.sliceArray(IntRange(0, 3)))
            var clipboardMessage: ClipBoardMessage? = null
            var baseInfoByte = ArrayList<Byte>(baseInfoLen)
            var i = 0
            var includeLast = 0
            while (receiveMap[msgKey]!![(receiveIndexMap[msgKey]!!.index + i) % UdpWindowMaxLen] != null) {
                val realIndex = (receiveIndexMap[msgKey]!!.index + i) % UdpWindowMaxLen
                msgBuf = receiveMap[msgKey]!![realIndex]!!
                if (i == 0) {
                    msgBuf = msgBuf.sliceArray(IntRange(4, msgBuf.size - 1))
                }
                if (baseInfoByte.size + msgBuf.size < baseInfoLen) {
                    for (b in msgBuf.sliceArray(IntRange(0, msgBuf.size -1))) baseInfoByte.add(b)
                    i++
                    continue
                }
                if (baseInfoByte.size + msgBuf.size > baseInfoLen) {
                    receiveMap[msgKey]!![realIndex] = msgBuf.sliceArray(IntRange(baseInfoLen - baseInfoByte.size, msgBuf.size - 1))
                    includeLast = 0
                } else {
                    includeLast = 1
                }
                for (b in msgBuf.sliceArray(IntRange(0, baseInfoLen - baseInfoByte.size -1))) baseInfoByte.add(b)
                try {
                    val baseInfoStr = String(baseInfoByte.toByteArray())
                    clipboardMessage = JSON.parseObject(baseInfoStr, ClipBoardMessage::class.java)
                    resultMap[msgKey] = clipboardMessage
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                break
            }
            if (clipboardMessage != null) {
                for (j in 0 until i + includeLast) {
                    receiveMap[msgKey]!![(receiveIndexMap[msgKey]!!.index + j) % UdpWindowMaxLen] = null
                }
                receiveIndexMap[msgKey]!!.index += i + includeLast
            }
        }
    }

    private fun checkFinish(msgKey: String) {
        if (resultMap[msgKey] == null) return
        if (receiveIndexMap[msgKey]!!.index >= receiveIndexMap[msgKey]!!.total) {
            val msgObj = resultMap[msgKey]!!
            val millisTs = System.currentTimeMillis()
            msgObj.createTime = millisTs
            msgObj.updateTime = millisTs
            listener?.onReceiveMsg(ClipBoardMessage(0, msgObj.type, msgObj.content, msgObj.extra, msgObj.createTime, msgObj.updateTime))
            isFinishMap[msgKey] = true
            receiveMap.remove(msgKey)
            threadPool.schedule(ClearKeyTask(this, msgKey), 3, TimeUnit.MINUTES)
        }
    }

    fun sendClipboardMsg(msgObj: ClipBoardMessage) {
        synchronized(syncWorkerMap) {
            for (worker in syncWorkerMap.values) {
                worker.sendClipboardMsg(msgObj)
            }
        }
    }

    fun start() {
        isRunning = true
        listener?.onChangeDeviceNum(isRunning, 0)
    }

    fun close() {
        isRunning = false
        deviceNum = 0
        for (worker in syncWorkerMap.values) {
            worker.close()
        }
        syncWorkerMap.clear()
        listener?.onChangeDeviceNum(isRunning, 0)
    }
}