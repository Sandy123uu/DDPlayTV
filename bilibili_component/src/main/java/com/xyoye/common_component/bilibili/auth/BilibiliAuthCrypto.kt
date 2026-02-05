package com.xyoye.common_component.bilibili.auth

import com.xyoye.common_component.crypto.KeystoreAesGcmCrypto

internal object BilibiliAuthCrypto {
    private const val AES_KEY_ALIAS = "dandanplay.bilibili.auth.aes"

    val crypto: KeystoreAesGcmCrypto by lazy { KeystoreAesGcmCrypto(AES_KEY_ALIAS) }
}
