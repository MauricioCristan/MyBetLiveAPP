package com.mybetlive.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybetlive.data.*
import java.time.*
import java.time.format.DateTimeFormatter
@Composable internal fun ProfileScreen(state:AppState,prices:Map<String,StorePrice>,buy:(Plan)->Unit,restore:()->Unit,manage:()->Unit,
 onLogout:()->Unit,onNotifications:()->Unit,initialPage:String="profile"){
 val subscription=state.subscription
 val advertising=com.mybetlive.ads.LocalAdsController.current
 var page by rememberSaveable(initialPage) { mutableStateOf(initialPage) }
 val listState=rememberLazyListState()
 LaunchedEffect(page) { listState.scrollToItem(0) }
 BackHandler(page != "profile") { page="profile" }
 LazyColumn(Modifier.fillMaxSize().testTag("profile-list"),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item {
   if(page!="profile") TextButton(onClick={page="profile"}){LocalizedText("‹ Volver a Perfil")}
   LocalizedText(when(page){"plan"->"Mi plan";"plans"->"Planes";"subscription"->"Mi suscripción";else->"Perfil"},fontSize=26.sp,fontWeight=FontWeight.Bold)
  }
  if(page=="profile") {
  item {
   Card {Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
    PlayerAvatar(null,size=44.dp)
    Column {LocalizedText(state.username,fontSize=19.sp,fontWeight=FontWeight.SemiBold);LocalizedText("Tu cuenta MyBetLive",color=Muted,fontSize=12.sp)}
   }}
  }
  }
  if(page in listOf("profile","plan")) {
  item{
   Card{Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    LocalizedText("Mi plan",fontSize=17.sp,fontWeight=FontWeight.Bold)
    if(subscription==null){LocalizedText("No se pudo consultar tu plan. Actualiza para reintentar.")}
    else {
     LocalizedText(subscription.plan.name,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
     LocalizedText("${subscription.activeTrackers} / ${subscription.access.maxActiveTrackers?.toString()?:"sin límite"} seguimientos activos")
     if(page=="profile") {
      LocalizedText("Props de jugadores: ${if(subscription.access.playerProps)"incluidos" else "no incluidos"}")
      LocalizedText("Notificaciones: ${if(subscription.access.pushNotifications)"incluidas" else "no incluidas"}")
     }
    }
    if(page=="profile") TextButton(onClick={page="plan"}){LocalizedText("Ver mi plan")}
    else if(subscription!=null){PlanBenefits(subscription);TextButton(onClick={page="plans"}){LocalizedText("Ver otros planes")}}
   }}
  }
  }
  if(page=="profile") item {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick={page="plans"},modifier=Modifier.weight(1f)){LocalizedText("Planes")}
    OutlinedButton(onClick={page="subscription"},modifier=Modifier.weight(1f)){LocalizedText("Mi suscripción")}
   }
  }
  if(subscription!=null){
   if(page in listOf("profile","plan")) item {PlanUsage(subscription)}
   if(page in listOf("profile","subscription")) {
   if(page=="subscription" && subscription.subscriptions.none{it.active}) item {
    LocalizedText("Sin suscripción vigente. Conservas el acceso de ${subscription.plan.name}.")
    if(subscription.subscriptions.isNotEmpty())LocalizedText("Tus seguimientos guardados se conservan. Los permisos actuales se aplican al crear nuevas selecciones.",color=Muted)
   }
   items(subscription.subscriptions.filter{it.active},key={it.id}){s->
    Card{Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     LocalizedText(s.plan.name,fontWeight=FontWeight.SemiBold)
     LocalizedText(if(s.source=="MANUAL")"Acceso concedido por administración" else when(s.state){
      "CANCELED"->"Cancelada · acceso hasta el vencimiento";"IN_GRACE_PERIOD"->"Pago en período de gracia";else->"Suscripción de Google Play"
     })
     LocalizedText("Vence: "+runCatching{Instant.parse(s.expiresAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm"))}.getOrDefault(s.expiresAt))
    }}
   }
   }
   if(page in listOf("profile","plans","subscription")) {
   item{
    if(!subscription.billingEnabled)LocalizedText("Las suscripciones estarán disponibles cuando se definan los planes y se habiliten las compras.")
    else {
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      OutlinedButton(onClick=restore,enabled=!state.busy){LocalizedText("Restaurar compras")}
      TextButton(onClick=manage){LocalizedText("Gestionar")}
     }
    }
    state.billingMessage?.let{LocalizedText(it,modifier=Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.secondary)}
   }
   }
   if(page=="plans" && subscription.availablePlans.none{it.code!="FREE"}) item {
    Card {Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     LocalizedText("Nuevos planes próximamente",fontSize=21.sp,fontWeight=FontWeight.Bold)
     LocalizedText("Cuando estén disponibles, aquí verás sus beneficios y precio. Puedes continuar usando tu plan actual.",color=Muted)
     Button(onClick={},enabled=false){LocalizedText("Suscripción aún no disponible")}
    }}
   }
   if(page=="plans") {
   items(subscription.availablePlans.filter{it.code!="FREE"},key={it.id}){plan->
    val price=prices[plan.id]
    val ownsPlay=subscription.subscriptions.any{it.active&&it.source=="GOOGLE_PLAY"}
    Card{Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
     LocalizedText(plan.name,fontSize=22.sp,fontWeight=FontWeight.Bold)
     if(plan.description.isNotBlank())LocalizedText(plan.description)
     LocalizedText("${plan.maxActiveTrackers?.toString()?:"Sin límite de"} seguimientos activos")
     if(plan.playerProps)LocalizedText("Props de jugadores incluidos")
     if(plan.pushNotifications)LocalizedText("Notificaciones incluidas")
     if(price!=null){
      LocalizedText("${price.price} / ${price.period}",fontSize=23.sp,fontWeight=FontWeight.Bold)
      LocalizedText(if(price.autoRenewing)"Renovación automática. Puedes cancelar en Google Play." else "Acceso por ${price.period}.",fontSize=12.sp)
     }
     Button(onClick={buy(plan)},enabled=subscription.billingEnabled&&price!=null&&!state.busy&&!ownsPlay&&!state.offline,modifier=Modifier.fillMaxWidth()){
      LocalizedText(if(ownsPlay)"Gestiona tu suscripción actual" else if(price==null)"Próximamente" else "Suscribirme con Google Play")
     }
    }}
   }
   }
  }
  if(page=="profile") {
  item {
   Card {Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    LocalizedText("Notificaciones",fontWeight=FontWeight.Bold)
    LocalizedText("Administra permisos y avisos en este dispositivo.",color=Muted,fontSize=13.sp)
    OutlinedButton(onClick=onNotifications,modifier=Modifier.fillMaxWidth()){LocalizedText("Configurar notificaciones")}
   }}
  }
  if(advertising?.privacyRequired==true) item { OutlinedButton(onClick=advertising::privacyOptions,modifier=Modifier.fillMaxWidth()){LocalizedText("Privacidad de anuncios")} }
  item {OutlinedButton(onClick=onLogout,enabled=!state.busy,modifier=Modifier.fillMaxWidth()){LocalizedText("Cerrar sesión")}}
  }
 }
}
