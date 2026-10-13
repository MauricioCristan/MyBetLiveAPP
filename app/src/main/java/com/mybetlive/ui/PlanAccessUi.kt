package com.mybetlive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.*

@Composable internal fun PlanUsage(subscription: MySubscription) {
    val limit = subscription.access.maxActiveTrackers
    val count = subscription.activeTrackers
    val reached = limit != null && count >= limit
    val tone = if(reached) Color(ProgressTone.AMBER.argb) else Green
    Card(shape=RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        LocalizedText("${subscription.plan.name} · $count / ${limit?.toString() ?: "sin límite"} seguimientos activos",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
        if(limit != null) LinearProgressIndicator(progress={ (count.toFloat()/limit.coerceAtLeast(1)).coerceIn(0f,1f) },color=tone,modifier=Modifier.fillMaxWidth())
        if(subscription.reward?.creditAvailable==true)LocalizedText("1 seguimiento extra disponible por anuncio · úsalo antes de "+runCatching{java.time.Instant.parse(subscription.reward.creditExpiresAt).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM · HH:mm"))}.getOrDefault("su vencimiento"),color=Green,fontSize=12.sp)
        else if((subscription.reward?.activeExtra?:0)>0)LocalizedText("Incluye 1 seguimiento extra por anuncio, válido hasta terminar.",color=Green,fontSize=12.sp)
        if(reached) LocalizedText("Tus seguimientos continúan. Podrás añadir otro cuando tengas menos de $limit activos.",color=Muted,fontSize=12.sp)
    } }
}

@Composable internal fun PlanBenefits(subscription: MySubscription) {
    val access = subscription.access
    listOf("${access.maxActiveTrackers?.toString() ?: "Sin límite de"} seguimientos activos",
        "ML, totales y hándicap de equipos incluidos",
        "Props de jugadores: ${if(access.playerProps) "incluidos" else "no incluidos"}",
        "Notificaciones: ${if(access.pushNotifications) "incluidas" else "no incluidas"}",
        "Historial de tus seguimientos incluido").forEach { LocalizedText(it,fontSize=14.sp) }
}

@Composable internal fun AccessGate(title: String, message: String, onPlans: () -> Unit, onContinue: () -> Unit) {
    Card(shape=RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        LocalizedText(title,fontSize=21.sp,fontWeight=FontWeight.Bold)
        LocalizedText(message,color=Muted)
        Button(onClick=onPlans,modifier=Modifier.fillMaxWidth()) { LocalizedText("Consultar planes") }
        OutlinedButton(onClick=onContinue,modifier=Modifier.fillMaxWidth()) { LocalizedText("Continuar con mi plan") }
    } }
}
