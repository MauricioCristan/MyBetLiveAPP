package com.mybetlive.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UnpinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("betId") ?: return
        BetNotifications(context).unpin(id)
    }
}
