package com.mybetlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mybetlive.BuildConfig
import com.mybetlive.R
import com.mybetlive.auth.googleIdToken
import com.mybetlive.data.*
import java.time.*
import java.time.format.DateTimeFormatter

internal val Ink = Color(0xFF0C141B)
internal val Panel = Color(0xFF17232E)
internal val Green = Color(0xFFB5F36D)
internal val Muted = Color(0xFF9AAEBB)
internal val Red = Color(0xFFFF9B91)

@Composable internal fun MyBetLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, onPrimary = Ink,
        primaryContainer = Color(0xFF263A21), onPrimaryContainer = Green,
        background = Ink, surface = Panel, surfaceContainer = Panel, surfaceContainerHigh = Panel,
        surfaceContainerHighest = Panel, secondary = Green, onSecondary = Ink,
        secondaryContainer = Color(0xFF263A21), onSecondaryContainer = Green,
        onSurface = Color(0xFFE8F0F4), onSurfaceVariant = Muted, error = Red), content = content)
}

@Composable
fun MyBetLiveApp(state: AppState, model: TrackingViewModel, prices: Map<String,StorePrice>,
    buy: (Plan) -> Unit, restore: () -> Unit, manage: () -> Unit) {
    MyBetLiveTheme {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            if (!state.loggedIn) LoginScreen(state, model) else Dashboard(state, model, prices, buy, restore, manage)
        }
    }
}

@Composable private fun LoginScreen(state: AppState, model: TrackingViewModel) {
    val context = LocalContext.current
    var url by rememberSaveable { mutableStateOf(if (BuildConfig.DEBUG) "http://127.0.0.1:8081" else "") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleAttempt by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(46.dp))
        BrandLogo(height = 52.dp)
        Text("Tu apuesta.\nCada jugada.", fontSize = 38.sp, lineHeight = 43.sp, fontWeight = FontWeight.Bold)
        Text("Sigue el marcador y el progreso de tus apuestas NFL, de fútbol y MLB en un solo lugar.", color = Muted)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(url, { url = it }, label = { Text("URL del backend") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        OutlinedButton(onClick = { googleAttempt = true; model.loginGoogle(url) { googleIdToken(context, it) } },
            enabled = !state.busy && url.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F))) {
            Icon(painterResource(R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Continuar con Google")
        }
        if (googleAttempt) state.error?.let { Text(it, color = Red) }
        Text("Si es tu primera vez, crearemos tu cuenta al continuar con Google.", color = Muted, fontSize = 12.sp)
        Text("O usa tu usuario y contraseña", color = Muted, fontSize = 12.sp)
        OutlinedTextField(username, { username = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        if (!googleAttempt) state.error?.let { Text(it, color = Red) }
        Button(onClick = { googleAttempt = false; model.login(url, username, password) }, enabled = !state.busy && username.isNotBlank() && password.isNotBlank() && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (state.busy) "Conectando…" else "Entrar") }
        Text("Registra apuestas que ya realizaste. Los resultados se basan en datos deportivos y pueden diferir de la liquidación del casino.", color = Muted, fontSize = 12.sp)
        if (BuildConfig.DEBUG) Text("Demo local: activa el perfil demo del backend y entra con demo / demo-change-me.", color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun Dashboard(state: AppState, model: TrackingViewModel, prices: Map<String,StorePrice>,
    buy: (Plan) -> Unit, restore: () -> Unit, manage: () -> Unit) {
    var eventId by rememberSaveable { mutableStateOf<String?>(null) }
    var createdBet by remember { mutableStateOf<BetView?>(null) }
    var savedMessage by remember { mutableStateOf(false) }
    DashboardScreen(state, prices, onRefresh = model::refresh, onSport = model::selectSport,
        onDate = model::date, onOpenEvent = { eventId = it }, onDelete = model::delete,
        onLogout = model::logout, buy = buy, restore = restore, manage = manage, createdBet = createdBet,
        onUnpin = model::unpin, onPin = model::pin)
    state.events.find { it.id == eventId }?.let { event ->
        CreateBetDialog(event, state, model, onDismiss = { eventId = null },
            onSaved = { bet -> eventId = null; createdBet = bet; savedMessage = true })
    }
    if(savedMessage) AlertDialog(onDismissRequest={savedMessage=false},title={Text("Seguimiento guardado")},
        text={Text(createdBet?.title().orEmpty())},confirmButton={TextButton(onClick={savedMessage=false}){Text("Ver seguimientos")}})
}

@Composable internal fun Scoreboard(event: EventSnapshot) {
    val label = when(event.status) {
        "FINAL" -> "Finalizado"; "SCHEDULED" -> "Por comenzar"; "LIVE" -> "En juego · ${event.period} ${event.clock.orEmpty()}"
        "CANCELLED" -> "Cancelado"; "POSTPONED" -> "Pospuesto"; else -> "Estado por confirmar"
    }
    event.leagueName?.takeIf { it.isNotBlank() }?.let { Text(listOfNotNull(it,event.leagueCountry).joinToString(" · "), color = Muted, fontSize = 12.sp) }
    Text("${sportLabel(event.sport)} · $label", color = if (event.status == "LIVE") Green else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(event.home to event.homeScore, event.away to event.awayScore).forEach { (team, score) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(team)
                Text(team.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(score?.toString() ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable private fun Choice(label: String, value: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("$label: ${options.find { it.first == value }?.second ?: "Seleccionar"}", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onSelected(id); expanded = false }) }
        }
    }
}

@Composable private fun PlayerChoice(value: String, players: List<Player>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = players.find { it.id == value }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = players.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlayerAvatar(selected?.imageUrl, size = 42.dp)
                Text("Jugador: ${selected?.name ?: "Seleccionar"}", modifier = Modifier.weight(1f))
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            players.forEach { player ->
                DropdownMenuItem(text = { Text(player.name) }, leadingIcon = { PlayerAvatar(player.imageUrl, size = 36.dp) },
                    onClick = { onSelected(player.id); expanded = false })
            }
        }
    }
}

@Composable private fun CreateBetDialog(event: EventView, state: AppState, model: TrackingViewModel, onDismiss: () -> Unit, onSaved: (BetView) -> Unit) {
    var market by rememberSaveable(event.id) { mutableStateOf("MONEYLINE") }
    var selection by rememberSaveable(event.id, market) { mutableStateOf(if (market == "MONEYLINE" || market == "SPREAD") "HOME" else "OVER") }
    var line by rememberSaveable(event.id, market) { mutableStateOf("") }
    var team by rememberSaveable(event.id) { mutableStateOf("HOME") }
    var playerId by rememberSaveable(event.id, market) { mutableStateOf("") }
    var stake by rememberSaveable { mutableStateOf("") }
    var odds by rememberSaveable { mutableStateOf("") }
    var bookmaker by rememberSaveable { mutableStateOf("") }
    var showDetails by rememberSaveable(event.id) { mutableStateOf(false) }
    var showPlans by rememberSaveable(event.id) { mutableStateOf(false) }
    var showLockedProps by rememberSaveable(event.id) { mutableStateOf(false) }
    val subscription = state.subscription
    val advertising=com.mybetlive.ads.LocalAdsController.current
    val rewardUnit=com.mybetlive.ads.AdsPolicy.rewardedUnitId(subscription,state.offline,com.mybetlive.BuildConfig.DEBUG,com.mybetlive.BuildConfig.ADMOB_APP_ID)
    val reachedLimit = subscription?.access?.maxActiveTrackers?.let { subscription.activeTrackers >= it } == true
    val isPlayer = market.startsWith("PLAYER_")
    val markets = (state.sports.find { it.sport == event.event.sport }?.markets ?: listOf("MONEYLINE"))
        .filter { !it.startsWith("PLAYER_") || state.subscription?.access?.playerProps != false }
    val players = if (state.playersEventId == event.id) state.players.filter { it.supports(market) } else emptyList()
    LaunchedEffect(isPlayer, event.id) { if (isPlayer) model.players(event.id) }
    val parsedLine = line.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val parsedStake = stake.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val parsedOdds = odds.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val valid = (market == "MONEYLINE" || parsedLine != null && (market == "SPREAD" || parsedLine >= 0) &&
        (event.event.sport !in listOf("SOCCER", "MLB", "NBA") || (parsedLine * 2) % 1.0 == 0.0)) &&
        (!isPlayer || players.any { it.id == playerId }) &&
        (stake.isBlank() || parsedStake != null && parsedStake > 0) && (odds.isBlank() || parsedOdds != null && parsedOdds > 1)
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Nuevo seguimiento", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Cerrar") }
                }
                Scoreboard(event.event)
                if(subscription!=null) PlanUsage(subscription)
                state.rewardMessage?.let { Text(it,color=Green,fontSize=13.sp) }
                state.error?.let { Text(it,color=Red) }
                if(showPlans) {
                    Text("Planes disponibles",fontSize=22.sp,fontWeight=FontWeight.Bold)
                    subscription?.availablePlans?.forEach { plan ->
                        Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(plan.name,fontWeight=FontWeight.Bold)
                            Text("${plan.maxActiveTrackers?.toString() ?: "Sin límite de"} seguimientos activos")
                            Text("Props ${if(plan.playerProps) "incluidas" else "no incluidas"} · notificaciones ${if(plan.pushNotifications) "incluidas" else "no incluidas"}")
                        } }
                    }
                    Text("Consulta Perfil → Planes para revisar disponibilidad y condiciones.",color=Muted,fontSize=13.sp)
                    OutlinedButton(onClick={showPlans=false;showLockedProps=false}){Text("Volver al partido")}
                } else if(reachedLimit) {
                    if(subscription?.reward?.eligible==true&&rewardUnit!=null&&advertising!=null){
                        Card{Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            Text("Desbloquea 1 seguimiento extra",fontWeight=FontWeight.Bold)
                            Text("Completa un anuncio para añadir otro seguimiento de equipos. Máximo uno extra activo; continuará hasta que termine. Tienes 24 horas para usar la recompensa.",color=Muted)
                            Button(onClick={model.watchReward(advertising::watchReward)},enabled=!state.busy&&advertising.ready,modifier=Modifier.fillMaxWidth()){
                                Text(if(state.busy) "Procesando anuncio…" else if(!advertising.ready) "Preparando anuncios…" else "Ver anuncio y desbloquear")
                            }
                        }}
                    }
                    AccessGate("Límite de seguimientos", "Tus seguimientos actuales continúan. Podrás añadir otra selección cuando se libere un espacio.",
                        onPlans={showPlans=true},onContinue=onDismiss)
                } else if(showLockedProps) {
                    AccessGate("Props de jugadores", "Tu plan no incluye nuevas props. Puedes seguir ML, totales y hándicap del equipo.",
                        onPlans={showPlans=true},onContinue={showLockedProps=false})
                } else {
                if (event.event.status == "FINAL") Text("Partido finalizado · puedes registrar tu apuesta y consultar su resultado.", color = Muted, fontSize = 12.sp)
                Text("¿Qué quieres seguir?", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("RESULT" to "Resultado", "TOTAL" to "Totales", "PLAYER" to "Jugadores").forEach { (group, label) ->
                        val options = markets.filter { marketGroup(it) == group }
                        val propsLocked = group=="PLAYER" && subscription?.access?.playerProps==false &&
                            state.sports.find{it.sport==event.event.sport}?.markets?.any{it.startsWith("PLAYER_")}==true
                        if (options.isEmpty() && !propsLocked) return@forEach
                        FilterChip(selected = marketGroup(market) == group, onClick = { if(propsLocked) showLockedProps=true else options.firstOrNull()?.let { market = it } },
                            enabled = (options.isNotEmpty() || propsLocked) && !state.busy, label = { Text(if(propsLocked) "$label · Premium" else label) })
                    }
                }
                Choice("Mercado", market, markets.filter { marketGroup(it) == marketGroup(market) }
                    .map { it to marketLabel(it,event.event.sport) }) { market = it }
                val sides = listOf("HOME" to event.event.home.name, "AWAY" to event.event.away.name)
                val selections = if(event.event.sport == "SOCCER" && market == "MONEYLINE") sides + ("DRAW" to "Empate") else sides
                Choice("Selección", selection, if (market in listOf("MONEYLINE", "SPREAD")) selections else listOf("OVER" to "Más de (Over)", "UNDER" to "Menos de (Under)")) { selection = it }
                if (market == "TEAM_TOTAL") Choice("Equipo", team, sides) { team = it }
                if (isPlayer) {
                    PlayerChoice(playerId, players) { playerId = it }
                    if (players.isEmpty()) Text(if (state.busy) "Cargando jugadores…" else "Aún no hay jugadores con datos para esta estadística.", color = Muted)
                }
                if (market != "MONEYLINE") OutlinedTextField(line, { line = it }, label = { Text(if (market == "SPREAD") "Hándicap (ej. -1.5)" else if(event.event.sport == "MLB") "Carreras (ej. 7.5)" else if(event.event.sport == "SOCCER") "Línea (ej. 2.5)" else "Línea (ej. 34.5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                if(event.event.sport in listOf("SOCCER", "MLB", "NBA") && parsedLine != null && (parsedLine * 2) % 1.0 != 0.0)
                    Text("Usa una línea entera o media: 1, 1.5, 2, 2.5…",color=Red,fontSize=12.sp)
                val draft = CreateBet(event.id, market, selection, if (market == "MONEYLINE") null else parsedLine,
                    if (market == "TEAM_TOTAL") team else null, if (isPlayer) playerId else null, parsedStake, parsedOdds, bookmaker.takeIf { it.isNotBlank() })
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tu selección", color = Muted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val selectedPlayer = players.find { it.id == playerId }
                            if (isPlayer && selectedPlayer != null) PlayerAvatar(selectedPlayer.imageUrl)
                            Text(draft.title(event.event, selectedPlayer?.name), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                    }
                }
                TextButton(onClick = { showDetails = !showDetails }) { Text(if (showDetails) "Ocultar datos opcionales" else "Añadir monto, momio o casino") }
                if (showDetails) {
                    OutlinedTextField(stake, { stake = it }, label = { Text("Monto · opcional") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(odds, { odds = it }, label = { Text("Momio decimal · opcional (1.72)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bookmaker, { if (it.length <= 80) bookmaker = it }, label = { Text("Casino · opcional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Button(onClick = { model.create(draft, onSaved) },
                    enabled = valid && !state.busy, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (state.busy) "Guardando…" else "Guardar seguimiento") }
                Text(when(event.event.sport) {
                    "SOCCER" -> "Se evalúan 90 minutos más compensación, sin prórroga ni penales. Se admiten líneas enteras o medias; revisa las reglas de tu apuesta."
                    "MLB" -> "Juego completo, incluidas entradas extra; sin condición de pitcher ni mercados de primeras entradas. Finales abreviados o datos incompletos esperan revisión. API-Baseball no ofrece props de jugadores."
                    else -> "El seguimiento usa el resultado del partido completo, incluido tiempo extra. Revisa que coincida con las reglas de tu apuesta."
                }, color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}
private fun marketGroup(market: String) = when {
    market.startsWith("PLAYER_") -> "PLAYER"
    market == "TOTAL" || market == "TEAM_TOTAL" -> "TOTAL"
    else -> "RESULT"
}
internal fun kickoff(value: String) = runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")) }.getOrDefault(value)
internal fun updatedTime(value: String) = runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")) }.getOrDefault("—")
