package com.musketeer.superclipboard.utils

import java.security.MessageDigest

fun ByteArray.toHex() = this.joinToString(separator = "") { it.toInt().and(0xff).toString(16).padStart(2, '0') }

object HashUtils {
    fun sha256(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(data.toByteArray())
        return md.digest().toHex()
    }

    fun md5(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(data.toByteArray())
        return md.digest().toHex()
    }
}