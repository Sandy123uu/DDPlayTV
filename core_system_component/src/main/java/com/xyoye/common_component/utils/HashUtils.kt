package com.xyoye.common_component.utils

import com.xyoye.common_component.extension.toHexString
import java.security.MessageDigest

object HashUtils {
    public fun md5Hex(input: String): String {
        val messageDigest = MessageDigest.getInstance("MD5")
        messageDigest.update(input.toByteArray())
        return messageDigest.digest().toHexString()
    }
}
