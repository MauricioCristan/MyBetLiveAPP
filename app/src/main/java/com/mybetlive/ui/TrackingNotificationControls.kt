package com.mybetlive.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.R
import com.mybetlive.data.*
import com.mybetlive.presentation.ProgressTrackRenderer

@Composable internal fun TrackingNotificationControls(bet: BetView, state: AppState,
    onPin: () -> Unit, onUnpin: () -> Unit, onSettings: () -> Unit) {
    var preview by remember(bet.id) { mutableStateOf(false) }
    val final=bet.status in settledStatuses
    val included=state.subscription?.access?.pushNotifications==true
    val pinned=!final && included && state.deviceNotificationsEnabled && bet.id in state.pinnedBets
    val unpinned=bet.id in state.unpinnedBets
    val heading=when {
        final -> "Notificación de resultado"
        state.subscription==null -> "Comprobando acceso a notificaciones"
        !included -> "Notificaciones no incluidas en tu plan"
        !state.deviceNotificationsEnabled -> "Notificaciones bloqueadas en Android"
        bet.status=="PENDING" -> "Aviso pendiente de inicio"
        pinned -> "Anclada en este dispositivo"
        unpinned -> "Notificación desanclada"
        else -> "Notificación sin anclar"
    }
    val description=when {
        final -> "Al finalizar, el aviso deja de ser permanente y puede descartarse."
        state.subscription==null -> "Actualiza para consultar los permisos de tu plan. Tu seguimiento se conserva."
        !included -> "Puedes consultar el avance aquí. Revisa los beneficios disponibles en Mi plan."
        !state.deviceNotificationsEnabled -> "Habilita los avisos de MyBetLive y el canal Seguimiento en vivo en los ajustes del dispositivo."
        bet.status=="PENDING" -> "El aviso aparecerá cuando el partido tenga datos. No se muestra un marcador inventado."
        pinned -> "Puedes consultar el avance desde la barra de notificaciones. El resultado sigue siendo provisional."
        unpinned -> "La apuesta continúa en la app. El aviso no se volverá a fijar automáticamente."
        else -> "Ancla este seguimiento para ver su avance en la barra de notificaciones."
    }
    Card(colors=CardDefaults.cardColors(containerColor=if(pinned) Color(0xFF153B2D) else Panel)) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(pinned) Icon(painterResource(R.drawable.ic_pin),null,tint=Green,modifier=Modifier.size(20.dp))
                Text(heading,fontWeight=FontWeight.SemiBold,color=if(pinned) Green else MaterialTheme.colorScheme.onSurface,
                    modifier=Modifier.testTag("notification-state"))
            }
            Text(description,color=Muted,fontSize=13.sp)
            if(!final && included) {
                when {
                    !state.deviceNotificationsEnabled -> Button(onClick=onSettings,modifier=Modifier.fillMaxWidth()) { Text("Habilitar notificaciones en Android") }
                    pinned -> OutlinedButton(onClick=onUnpin,enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("unpin-notification")) { Text("Desanclar notificación") }
                    bet.status!="PENDING" -> Button(onClick=onPin,enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("pin-notification")) { Text(if(unpinned) "Volver a anclar" else "Anclar notificación") }
                }
            }
            OutlinedButton(onClick={preview=true},modifier=Modifier.fillMaxWidth().testTag("notification-preview")) { Text("Ver vista previa del aviso") }
            if(!final) Text("Desanclar conserva el seguimiento y su historial.",color=Muted,fontSize=11.sp)
        }
    }
    if(preview) NotificationPreviewDialog(bet,state.offline) { preview=false }
}

/** Uses the actual tracker. Android draws the panel, lock screen and native Live Update template. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun NotificationPreviewDialog(bet: BetView, offline: Boolean, onDismiss: () -> Unit) {
    var variant by remember { mutableIntStateOf(1) }
    val p=bet.presentation(offline)
    val tone=Color(p.tone.argb)
    val context=LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,modifier=Modifier.testTag("notification-preview-dialog"),title={Text("Vista previa del aviso")},
        text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("El aspecto en la barra y el bloqueo depende de tu versión de Android y sus ajustes de privacidad.",color=Muted,fontSize=12.sp)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Compacta","Ampliada","Privada").forEachIndexed { index,label ->
                    FilterChip(selected=variant==index,onClick={variant=index},label={Text(label,fontSize=12.sp)},modifier=Modifier.testTag("preview-$index"))
                }
            }
            Card(colors=CardDefaults.cardColors(containerColor=Ink)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(if(variant==2) R.drawable.ic_notification_ball else ProgressTrackRenderer.notificationIcon(bet.event.sport)),null,tint=Muted,modifier=Modifier.size(16.dp))
                        Text("MyBetLive",fontWeight=FontWeight.SemiBold,fontSize=12.sp)
                    }
                    if(variant==2) {
                        Text("Contenido oculto · desbloquea para ver tu seguimiento",color=Muted,modifier=Modifier.testTag("private-preview"))
                    } else {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            if(variant==1 && bet.market.startsWith("PLAYER_")) PlayerAvatar(bet.playerImageUrl,size=44.dp)
                            Text(bet.title(),fontWeight=FontWeight.SemiBold,fontSize=14.sp,maxLines=if(variant==0) 2 else 4,
                                overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                        }
                        if(variant==1) MarketCardContent(bet,offline,compact=true)
                        else {
                            Text("${sportLabel(bet.event.sport)} · ${bet.event.period} ${bet.event.clock.orEmpty()}",color=Muted,fontSize=11.sp)
                            Text(p.value,color=tone,fontWeight=FontWeight.Bold,fontSize=22.sp)
                            bet.visualProgress(offline)?.let { progress ->
                                Canvas(Modifier.fillMaxWidth().height(26.dp)) {
                                    ProgressTrackRenderer.draw(context,drawContext.canvas.nativeCanvas,size.width,size.height,progress,ProgressTone.MUTED.argb,compact=true)
                                }
                            }
                            Text(p.status,color=tone,fontSize=12.sp)
                            Text("Último dato ${updatedTime(bet.updatedAt)}",color=Muted,fontSize=10.sp)
                        }
                    }
                }
            }
            Text(if(bet.status in settledStatuses) "Resultado final · aviso descartable" else "Seguimiento provisional · aviso permanente mientras esté anclado",color=Muted,fontSize=11.sp)
        }},confirmButton={TextButton(onClick=onDismiss) { Text("Cerrar") }})
}
