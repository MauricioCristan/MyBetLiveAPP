package com.mybetlive.notifications

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotificationArtworkTest {
    @Test fun svgArtworkIsRasterizedIntoABoundedNotificationBitmap() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="40" fill="#ff0000"/></svg>"""
        val bitmap = NotificationArtwork.decode(svg.toByteArray(), true)!!
        assertEquals(160, bitmap.width); assertEquals(160, bitmap.height)
        assertEquals(Color.RED, bitmap.getPixel(80,80)); assertEquals(Color.TRANSPARENT,bitmap.getPixel(0,0))
    }
    @Test fun invalidAndOversizedMediaUseTheFallbackAndPngStillDecodes() {
        assertNull(NotificationArtwork.decode("not svg".toByteArray(), true))
        assertNull(NotificationArtwork.decode(ByteArray(512_001), true))
        assertNull(NotificationArtwork.decode("not png".toByteArray()))
        val original = Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888)
        val out = java.io.ByteArrayOutputStream();original.compress(Bitmap.CompressFormat.PNG,100,out)
        assertNotNull(NotificationArtwork.decode(out.toByteArray()))
    }
}
