package com.example.ppapb_uas.utils

import java.security.SecureRandom

object Utils {
    fun randomString(length: Int = 12): String {
        return ByteArray(length).let {
            SecureRandom().nextBytes(it)
            it.joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}