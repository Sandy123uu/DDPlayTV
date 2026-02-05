package com.xyoye.common_component.extension

import android.util.Base64
import com.xyoye.common_component.utils.EntropyUtils

fun String?.toMd5String(): String = EntropyUtils.string2Md5(this)

fun String.aesEncode(
    key: String,
    version: Int = EntropyUtils.AES_VERSION_GCM_V2,
): String? = EntropyUtils.aesEncode(key, this, Base64.NO_WRAP, version = version)

fun String.aesDecode(
    key: String,
    allowLegacyDefaultKeyFallback: Boolean = false,
): String? =
    EntropyUtils.aesDecode(
        key,
        this,
        Base64.NO_WRAP,
        allowLegacyDefaultKeyFallback = allowLegacyDefaultKeyFallback,
    )
