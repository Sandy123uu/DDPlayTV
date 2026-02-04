package com.xyoye.common_component.network.cloud115

import com.xyoye.common_component.utils.JsonHelper
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeLoginResp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Cloud115ModelsMoshiTest {
    @Test
    fun qrcodeLogin_allowsLargeIsVipValue() {
        val json =
            """
            {
              "state": 1,
              "data": {
                "user_id": 37610873,
                "user_name": "37610873",
                "is_vip": 4294967295,
                "cookie": {
                  "UID": "37610873_I1_1769999716",
                  "CID": "031f060a87130cd7619772d2e9500c03",
                  "SEID": "seid",
                  "KID": "kid"
                },
                "face": {
                  "face_l": "https://avatars.115.com/01/bgx656_l.jpg?v=1769999716",
                  "face_m": "https://avatars.115.com/01/bgx656_m.jpg?v=1769999716",
                  "face_s": "https://avatars.115.com/01/bgx656_s.jpg?v=1769999716"
                }
              }
            }
            """.trimIndent()

        val adapter = JsonHelper.MO_SHI.adapter(Cloud115QRCodeLoginResp::class.java)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(1, parsed!!.state)
        assertNotNull(parsed.data)
        assertEquals(37610873L, parsed.data!!.userId)
        assertEquals(4294967295L, parsed.data!!.isVip)
        assertNotNull(parsed.data!!.cookie)
        assertEquals("37610873_I1_1769999716", parsed.data!!.cookie!!.uid)
    }
}
