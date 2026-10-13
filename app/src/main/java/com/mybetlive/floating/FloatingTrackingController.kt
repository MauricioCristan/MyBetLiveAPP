package com.mybetlive.floating

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mybetlive.application.FloatingTracking
import com.mybetlive.application.trackingOwner
import com.mybetlive.composition.ClientDependencies
import com.mybetlive.data.settledStatuses

class FloatingTrackingController(private val context: Context) : FloatingTracking {
    override fun activate(id: String) {
        check(Settings.canDrawOverlays(context)) { "Habilita el permiso para mostrar la burbuja sobre otras aplicaciones." }
        val repository=ClientDependencies.repository(context)
        val owner=repository.session?.trackingOwner() ?: error("Inicia sesión")
        ClientDependencies.live.bind(owner,repository.cachedBets)
        val pick=ClientDependencies.live.state.value.bets.find { it.id==id } ?: error("Este seguimiento ya no está disponible.")
        check(pick.status !in settledStatuses) { "Este seguimiento ya finalizó." }
        ContextCompat.startForegroundService(context,Intent(context,FloatingTrackingService::class.java).putExtra("betId",id).putExtra("owner",owner))
    }
    override fun stop() { context.stopService(Intent(context,FloatingTrackingService::class.java)) }
}
