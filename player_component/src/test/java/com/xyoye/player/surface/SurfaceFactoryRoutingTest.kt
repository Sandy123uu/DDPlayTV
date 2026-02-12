package com.xyoye.player.surface

import com.xyoye.data_component.enums.PlayerType
import com.xyoye.data_component.enums.SurfaceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceFactoryRoutingTest {
    @Test
    fun vlcRespectsTextureSurfaceSwitch() {
        val textureFactory =
            SurfaceFactory.getFactory(
                PlayerType.TYPE_VLC_PLAYER,
                SurfaceType.VIEW_TEXTURE
            )
        val surfaceFactory =
            SurfaceFactory.getFactory(
                PlayerType.TYPE_VLC_PLAYER,
                SurfaceType.VIEW_SURFACE
            )

        assertTrue(textureFactory is VLCViewFactory)
        assertTrue(surfaceFactory is VLCViewFactory)
        assertTrue(readUseTextureView(textureFactory as VLCViewFactory))
        assertFalse(readUseTextureView(surfaceFactory as VLCViewFactory))
    }

    @Test
    fun nonVlcRoutingRemainsUnchanged() {
        val mpvSurface =
            SurfaceFactory.getFactory(
                PlayerType.TYPE_MPV_PLAYER,
                SurfaceType.VIEW_SURFACE
            )
        val mpvTexture =
            SurfaceFactory.getFactory(
                PlayerType.TYPE_MPV_PLAYER,
                SurfaceType.VIEW_TEXTURE
            )
        val media3Surface =
            SurfaceFactory.getFactory(
                PlayerType.TYPE_EXO_PLAYER,
                SurfaceType.VIEW_SURFACE
            )
        val media3Texture =
            SurfaceFactory.getFactory(
                PlayerType.TYPE_EXO_PLAYER,
                SurfaceType.VIEW_TEXTURE
            )

        assertTrue(mpvSurface is MpvSurfaceViewFactory)
        assertTrue(mpvTexture is MpvViewFactory)
        assertTrue(media3Surface is SurfaceViewFactory)
        assertTrue(media3Texture is TextureViewFactory)
    }

    private fun readUseTextureView(factory: VLCViewFactory): Boolean {
        val field = factory.javaClass.getDeclaredField("useTextureView")
        field.isAccessible = true
        return field.getBoolean(factory)
    }
}
