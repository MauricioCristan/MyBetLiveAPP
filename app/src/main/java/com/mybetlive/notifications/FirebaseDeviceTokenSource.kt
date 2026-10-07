package com.mybetlive.notifications
import android.content.Context
import com.mybetlive.application.DeviceTokenSource
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
class FirebaseDeviceTokenSource(private val context:Context):DeviceTokenSource {
    override suspend fun token():String?=if(FirebaseApp.getApps(context).isEmpty())null else FirebaseMessaging.getInstance().token.await()
}
