package com.mybetlive.floating

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class FloatingServiceSafetyTest {
    @Test fun android15ServiceDeclaresSpecialUseAndIsNotExported() {
        val context=RuntimeEnvironment.getApplication()
        val info=context.packageManager.getServiceInfo(ComponentName(context,FloatingTrackingService::class.java),0)
        assertFalse(info.exported)
        assertEquals(0,info.flags and ServiceInfo.FLAG_STOP_WITH_TASK)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,info.foregroundServiceType)
    }
    @Test @Config(sdk=[36]) fun android16RejectsUnrequestedServiceStartWithoutAnOwner() {
        val service=Robolectric.buildService(FloatingTrackingService::class.java,Intent()).create()
        assertEquals(android.app.Service.START_NOT_STICKY,service.get().onStartCommand(Intent(),0,1))
        service.destroy()
    }
    @Test fun systemRecoveryWithoutTheSavedLoginIsRejectedAndCleared(){
        val context=RuntimeEnvironment.getApplication();val store=FloatingSessionStore(context)
        store.save("expired-account|generation",listOf("pick"),"pick")
        val service=Robolectric.buildService(FloatingTrackingService::class.java).create()
        assertEquals(android.app.Service.START_NOT_STICKY,service.get().onStartCommand(null,0,1))
        assertNull(store.read("expired-account|generation"));service.destroy()
    }
    @Test fun permissionDeniedDoesNotStartAnOverlay() {
        val context=RuntimeEnvironment.getApplication()
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
        assertThrows(IllegalStateException::class.java){FloatingTrackingController(context).activate("pick")}
    }
}
