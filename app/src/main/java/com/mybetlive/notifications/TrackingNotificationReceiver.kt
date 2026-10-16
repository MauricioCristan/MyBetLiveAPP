package com.mybetlive.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Explicit immutable PendingIntents only; switching uses saved snapshots without network requests. */
class TrackingNotificationReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val epoch=intent.getLongExtra("epoch",-1)
        val notifications=BetNotifications(context)
        when(intent.action){
            BetNotifications.PREVIOUS->notifications.navigate(-1,epoch)
            BetNotifications.NEXT->notifications.navigate(1,epoch)
            BetNotifications.DISMISS->notifications.dismissed(epoch)
        }
    }
}
