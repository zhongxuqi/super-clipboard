package com.musketeer.superclipboard.net

import android.util.Log
import com.alibaba.fastjson.JSON
import com.musketeer.superclipboard.ClipboardMainWindow
import com.musketeer.superclipboard.R
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class UdpClient {
    companion object {
        val Header: Byte = 0x00
        var Instance: UdpClient? = null
    }

    class MetaData {
        var udp_addrs: List<String>? = null
    }

    val client: DatagramSocket
    val buffer: ByteArray = ByteArray(1024)
    val packet: DatagramPacket
    val threadPool: ExecutorService

    var isRunning: Boolean = false
    var deviceNum: Int = 0

    constructor() {
        client = DatagramSocket()
        packet = DatagramPacket(buffer, buffer.size)
        threadPool = Executors.newFixedThreadPool(3)
    }

    fun start() {
        isRunning = true
        threadPool.submit(Runnable {
            while (isRunning) {
                client.receive(packet)
                if (packet.length < 2) continue
                val metaMessageLen = packet.data[1].toInt()
                if (packet.length < 2 + metaMessageLen) continue
                val strReceive = String(packet.data, 2, metaMessageLen)
                Log.d("UdpClient", strReceive + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort())
                packet.length = buffer.size
                try {
                    val metaData = JSON.parseObject(strReceive, MetaData::class.java)

                    var newDeviceNum = 1
                    if (metaData!!.udp_addrs != null) {
                        newDeviceNum = metaData.udp_addrs!!.size + 1
                    }
                    if (deviceNum != newDeviceNum) {
                        deviceNum = newDeviceNum
                        ClipboardMainWindow.Instance?.handler?.post {
                            ClipboardMainWindow.Instance?.syncStateTextView?.text = String.format(ClipboardMainWindow.Instance?.mContext!!.getString(R.string.device_total, deviceNum))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
        threadPool.submit(Runnable {
            while (isRunning) {
                val msg = "{\"app_id\":\"superclipboard\"}".toByteArray()
                val buffer: ByteArray = ByteArray(2 + msg.size)
                buffer[0] = Header
                buffer[1] = msg.size.toByte()
                msg.copyInto(buffer, 2)
                client.send(DatagramPacket(buffer, buffer.size, InetAddress.getByName("192.168.100.107"), 9000))
                Thread.sleep(1000)
            }
        })
    }

    fun close() {
        isRunning = false
    }
}