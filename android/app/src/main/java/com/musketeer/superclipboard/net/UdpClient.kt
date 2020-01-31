package com.musketeer.superclipboard.net

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class UdpClient {
    companion object {
        var Instance: UdpClient? = null
    }

    val client: DatagramSocket
    val buffer: ByteArray = ByteArray(1024)
    val packet: DatagramPacket
    var isRunning: Boolean = false
    val threadPool: ExecutorService

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
                if (packet.length <= 0) continue
                val strReceive = String(packet.getData(), 0, packet.getLength())
                Log.d("UdpClient", strReceive + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort())
                packet.length = buffer.size
            }
        })
        threadPool.submit(Runnable {
            while (isRunning) {
                val msg = "{\"app_id\":\"superclipboard\"}".toByteArray()
                client.send(DatagramPacket(msg, msg.size, InetAddress.getByName("192.168.100.107"), 9000))
                Thread.sleep(3000)
            }
        })
    }

    fun close() {
        isRunning = false
    }
}