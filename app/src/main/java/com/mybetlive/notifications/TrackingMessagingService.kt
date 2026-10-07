package com.mybetlive.notifications

import android.util.Base64
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonParser
import com.mybetlive.data.*
import kotlinx.coroutines.*

class TrackingMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val repository = com.mybetlive.composition.ClientDependencies.repository(applicationContext)
        repository.vault.fcmToken = token
        if (repository.vault.session != null) CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { repository.registerDevice() }
        }
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val session = SessionVault(this).session ?: return
        val owner = runCatching {
            val claims = session.tokens.accessToken.split('.')[1]
            JsonParser.parseString(String(Base64.decode(claims, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))).asJsonObject["sub"].asString
        }.getOrNull()
        if (owner != message.data["userId"]) return
        BetNotifications(this).show(message.data)
    }
}
