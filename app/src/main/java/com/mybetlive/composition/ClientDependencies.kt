package com.mybetlive.composition
import android.content.Context
import com.mybetlive.data.TrackingRepository
import com.mybetlive.notifications.FirebaseDeviceTokenSource
import com.mybetlive.auth.GoogleCredentialCleaner
object ClientDependencies {
    val live = com.mybetlive.application.LiveTrackingStore()
    val sync = com.mybetlive.application.LiveTrackingCoordinator()
    @Volatile private var shared: TrackingRepository? = null
    fun repository(context:Context):TrackingRepository = shared ?: synchronized(this) {
        shared ?: TrackingRepository(context.applicationContext,FirebaseDeviceTokenSource(context.applicationContext),GoogleCredentialCleaner(context.applicationContext)).also { shared=it }
    }
}
