package com.xyoye.open_cc

import org.junit.Test

class OpenCCJvmLoadTest {
    @Test
    fun `OpenCC object init should not load native library`() {
        // This intentionally does NOT call convertSC/convertTC, because those paths may require
        // Android runtime/app context for OpenCCFile and config paths.
        // The goal is to ensure the Kotlin object can be initialized on the JVM (unit tests)
        // without failing due to System.loadLibrary(...).
        OpenCC.toString()
    }
}

