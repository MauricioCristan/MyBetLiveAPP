package com.mybetlive.composition
import android.content.Context
import com.mybetlive.data.TrackingRepository
import com.mybetlive.notifications.FirebaseDeviceTokenSource
import com.mybetlive.auth.GoogleCredentialCleaner
object ClientDependencies {
    fun repository(context:Context)=TrackingRepository(context.applicationContext,FirebaseDeviceTokenSource(context.applicationContext),GoogleCredentialCleaner(context.applicationContext))
}
