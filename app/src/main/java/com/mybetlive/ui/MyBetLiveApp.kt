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
import com.mybetlive.application.propLineError
import com.mybetlive.application.propCondition
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
            if(state.emailLink!=null)EmailLinkScreen(state,model)
            else if (!state.loggedIn) LoginScreen(state, model) else Dashboard(state, model, prices, buy, restore, manage)
        }
    }
}

@Composable private fun LoginScreen(state: AppState, model: TrackingViewModel) {
    val context = LocalContext.current
    var url by rememberSaveable { mutableStateOf(state.authUrl ?: BuildConfig.AUTH_BACKEND_URL) }
    var mode by rememberSaveable { mutableStateOf(EmailFormMode.LOGIN) }
    fun changeMode(next:EmailFormMode) { model.clearEmailFeedback(); mode=next }
    androidx.activity.compose.BackHandler(enabled=mode!=EmailFormMode.LOGIN&&!state.busy) { changeMode(EmailFormMode.LOGIN) }
    LaunchedEffect(state.authUrl){state.authUrl?.let{url=it}}
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BrandLogo(height = 34.dp)
        Spacer(Modifier.height(6.dp))
        LocalizedText(when(mode){EmailFormMode.LOGIN->"Inicia sesión";EmailFormMode.REGISTER->"Crear cuenta";EmailFormMode.FORGOT->"Recuperar contraseña"}, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        if(mode==EmailFormMode.LOGIN) {
        LocalizedText("Sigue tus picks en tiempo real.", color = Muted, fontSize=14.sp)
        OutlinedButton(onClick = { model.clearEmailFeedback();model.loginGoogle(url) { googleIdToken(context, it) } },
            enabled = !state.busy && url.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F))) {
            Icon(painterResource(R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            LocalizedText("Continuar con Google")
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f),color=Muted.copy(alpha=.25f))
            LocalizedText("O entra con tu correo",color=Muted,fontSize=12.sp)
            HorizontalDivider(Modifier.weight(1f),color=Muted.copy(alpha=.25f))
        }
        }
        key(mode) { EmailAccountForm(state,model,url,mode,::changeMode) }
        if(mode==EmailFormMode.LOGIN) {
        LocalizedText("Registra apuestas que ya realizaste. Los resultados se basan en datos deportivos y pueden diferir de la liquidación del casino.", color = Muted, fontSize = 11.sp,lineHeight=16.sp)
        }
    }
}

@Composable private fun Dashboard(state: AppState, model: TrackingViewModel, prices: Map<String,StorePrice>,
    buy: (Plan) -> Unit, restore: () -> Unit, manage: () -> Unit) {
    var eventId by rememberSaveable { mutableStateOf<String?>(null) }
    var createdBet by remember { mutableStateOf<BetView?>(null) }
    var savedMessage by remember { mutableStateOf(false) }
    val context=LocalContext.current
    val bankEnabled=state.subscription?.bankAccess!=null || state.subscription==null && runCatching{com.mybetlive.composition.ClientDependencies.bankRepository(context).cached().banks.isNotEmpty()}.getOrDefault(false)
    DashboardScreen(state, prices, onRefresh = model::refresh, onSport = model::selectSport,
        onDate = model::date, onOpenEvent = { eventId = it }, onDelete = model::delete,
        onLogout = model::logout, buy = buy, restore = restore, manage = manage, createdBet = createdBet,
        bankContent=if(bankEnabled){{refresh,onPlans->BankRoute(state.bets,refresh,onPlans,state.subscription?.bankAccess)}}else null,
        onHistory=model::loadHistory,onHistoryMore=model::loadMoreHistory)
    LaunchedEffect(eventId, state.events) {
        if(eventId != null && state.events.none { it.id == eventId && it.event.canStartTracking() }) eventId = null
    }
    state.events.find { it.id == eventId && it.event.canStartTracking() }?.let { event ->
        CreateBetDialog(event, state, model, onDismiss = { eventId = null },
            onSaved = { bet -> eventId = null; createdBet = bet; savedMessage = true })
    }
    if(savedMessage) AlertDialog(onDismissRequest={savedMessage=false},title={LocalizedText("Seguimiento guardado")},
        text={LocalizedText(createdBet?.title().orEmpty())},confirmButton={TextButton(onClick={savedMessage=false}){LocalizedText("Ver seguimientos")}})
}

@Composable internal fun Scoreboard(event: EventSnapshot) {
    val now by produceState(java.time.Instant.now(),event.status,event.startsAt){
        if(event.status=="SCHEDULED")while(true){kotlinx.coroutines.delay(30_000);value=java.time.Instant.now()}
    }
    val label = when(event.status) {
        "FINAL" -> "Finalizado"; "SCHEDULED" -> event.scheduledLabel(now); "LIVE" -> "En juego · ${event.period} ${event.clock.orEmpty()}"
        "CANCELLED" -> "Cancelado"; "POSTPONED" -> "Pospuesto"; else -> "Estado por confirmar"
    }
    event.leagueName?.takeIf { it.isNotBlank() }?.let { LocalizedText(listOfNotNull(it,event.leagueCountry).joinToString(" · "), color = Muted, fontSize = 12.sp) }
    LocalizedText("${sportLabel(event.sport)} · $label", color = if (event.status == "LIVE") Green else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(event.home to event.homeScore, event.away to event.awayScore).forEach { (team, score) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(team)
                LocalizedText(team.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                LocalizedText(score?.toString() ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable private fun Choice(label: String, value: String, options: List<Pair<String, String>>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            LocalizedText("$label: ${options.find { it.first == value }?.second ?: "Seleccionar"}", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (id, text) -> DropdownMenuItem(text = { LocalizedText(text) }, onClick = { onSelected(id); expanded = false }) }
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
                LocalizedText("Jugador: ${selected?.name ?: "Seleccionar"}", modifier = Modifier.weight(1f))
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            players.forEach { player ->
                DropdownMenuItem(text = { LocalizedText(player.name) }, leadingIcon = { PlayerAvatar(player.imageUrl, size = 36.dp) },
                    onClick = { onSelected(player.id); expanded = false })
            }
        }
    }
}

@Composable private fun CreateBetDialog(event: EventView, state: AppState, model: TrackingViewModel, onDismiss: () -> Unit, onSaved: (BetView) -> Unit) {
    var builder by rememberSaveable(event.id){mutableStateOf(false)}
    var builderJson by rememberSaveable(event.id){mutableStateOf("[]")}
    val builderLegs=remember(builderJson){runCatching{com.google.gson.Gson().fromJson(builderJson,Array<TrackingBuilderSelection>::class.java).toList()}.getOrDefault(emptyList())}
    fun updateBuilder(legs:List<TrackingBuilderSelection>){builderJson=com.google.gson.Gson().toJson(legs)}
    var market by rememberSaveable(event.id) { mutableStateOf("MONEYLINE") }
    var selection by rememberSaveable(event.id, market) { mutableStateOf(when(market){"MONEYLINE","SPREAD","RESULT_3WAY"->"HOME";"DOUBLE_CHANCE"->"HOME_DRAW";"BOTH_TEAMS_SCORE","CORRECT_SCORE","OVERTIME"->"YES";else->"OVER"}) }
    var scope by rememberSaveable(event.id,market) { mutableStateOf(if(event.event.sport=="NHL")hockeyScopes(market).first() else "MATCH") }
    var scoreHome by rememberSaveable(event.id,market) { mutableStateOf("0") }
    var scoreAway by rememberSaveable(event.id,market) { mutableStateOf("0") }
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
        .filter { (!it.startsWith("PLAYER_") || state.subscription?.access?.playerProps != false)&&(!builder||!it.startsWith("PLAYER_")) }
    val players = if (state.playersEventId == event.id) state.players.filter { it.supports(market) } else emptyList()
    LaunchedEffect(isPlayer, event.id) { if (isPlayer) model.players(event.id) }
    val parsedLine = line.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val parsedStake = stake.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val parsedOdds = odds.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val addToBank=parsedStake!=null&&parsedStake>0&&parsedOdds!=null&&parsedOdds>1&&state.subscription?.bankAccess!=null
    var selectedBankId by rememberSaveable(event.id){mutableStateOf<String?>(null)}
    LaunchedEffect(addToBank,event.id){if(addToBank)model.loadTrackingBanks()}
    val destination=trackingBankDestination(state.trackingBanks,selectedBankId)
    val lineRule=state.sports.find { it.sport==event.event.sport }?.lineRules?.get(market)
    val needsLine=needsMarketLine(market)
    val lineError=if(needsLine&&line.isNotBlank())propLineError(line,lineRule) else null
    val valid = (!needsLine || parsedLine != null && (market == "SPREAD" || parsedLine >= 0) &&
        (event.event.sport !in listOf("SOCCER", "MLB", "NBA", "NHL") || (parsedLine * 2) % 1.0 == 0.0) && lineRule!=null && lineError==null) &&
        (!isPlayer || players.any { it.id == playerId }) &&
        (stake.isBlank() || parsedStake != null && parsedStake > 0) && (odds.isBlank() || parsedOdds != null && parsedOdds > 1)
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    LocalizedText("Nuevo seguimiento", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onDismiss, enabled = !state.busy) { LocalizedText("Cerrar") }
                }
                Scoreboard(event.event)
                if(subscription!=null) PlanUsage(subscription)
                state.rewardMessage?.let { LocalizedText(it,color=Green,fontSize=13.sp) }
                state.error?.let { LocalizedText(it,color=Red) }
                if(showPlans) {
                    LocalizedText("Planes disponibles",fontSize=22.sp,fontWeight=FontWeight.Bold)
                    subscription?.availablePlans?.forEach { plan ->
                        Card { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            LocalizedText(plan.name,fontWeight=FontWeight.Bold)
                            LocalizedText("${plan.maxActiveTrackers?.toString() ?: "Sin límite de"} seguimientos activos")
                            LocalizedText("Props ${if(plan.playerProps) "incluidas" else "no incluidas"} · notificaciones ${if(plan.pushNotifications) "incluidas" else "no incluidas"}")
                        } }
                    }
                    LocalizedText("Consulta Perfil → Planes para revisar disponibilidad y condiciones.",color=Muted,fontSize=13.sp)
                    OutlinedButton(onClick={showPlans=false;showLockedProps=false}){LocalizedText("Volver al partido")}
                } else if(reachedLimit) {
                    if(subscription?.reward?.eligible==true&&rewardUnit!=null&&advertising!=null){
                        Card{Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            LocalizedText("Desbloquea 1 seguimiento extra",fontWeight=FontWeight.Bold)
                            LocalizedText("Completa un anuncio para añadir otro seguimiento de equipos. Máximo uno extra activo; continuará hasta que termine. Tienes 24 horas para usar la recompensa.",color=Muted)
                            Button(onClick={model.watchReward(advertising::watchReward)},enabled=!state.busy&&advertising.ready,modifier=Modifier.fillMaxWidth()){
                                LocalizedText(if(state.busy) "Procesando anuncio…" else if(!advertising.ready) "Preparando anuncios…" else "Ver anuncio y desbloquear")
                            }
                        }}
                    }
                    AccessGate("Límite de seguimientos", "Tus seguimientos actuales continúan. Podrás añadir otra selección cuando se libere un espacio.",
                        onPlans={showPlans=true},onContinue=onDismiss)
                } else if(showLockedProps) {
                    AccessGate("Props de jugadores", "Tu plan no incluye nuevas props. Puedes seguir ML, totales y hándicap del equipo.",
                        onPlans={showPlans=true},onContinue={showLockedProps=false})
                } else {
                if(event.event.sport=="SOCCER"){
                    LocalizedText("Formato del seguimiento",fontWeight=FontWeight.SemiBold)
                    ScrollableChoices(if(builder)"BUILDER" else "SIMPLE",listOf("SIMPLE" to "Simple","BUILDER" to "Creador de apuestas · Premium"),{type->
                        if(type=="BUILDER"&&subscription?.bankAccess?.advanced!=true)showPlans=true
                        else {builder=type=="BUILDER";market="MONEYLINE";selection="HOME";line=""}
                    },enabled=!state.busy,tagPrefix="tracking-format")
                }
                if(builder)TrackingBuilderSelections(builderLegs,event.event,!state.busy){index->updateBuilder(builderLegs.filterIndexed{i,_->i!=index})}
                LocalizedText("¿Qué quieres seguir?", fontWeight = FontWeight.SemiBold)
                val propsLocked = !builder && subscription?.access?.playerProps==false &&
                    state.sports.find{it.sport==event.event.sport}?.markets?.any{it.startsWith("PLAYER_")}==true
                val groups = listOf("RESULT" to "Resultado", "TOTAL" to "Totales", "PLAYER" to "Jugadores")
                    .filter { (group, _) -> markets.any { marketGroup(it)==group } || group=="PLAYER" && propsLocked }
                    .map { (group,label) -> group to if(group=="PLAYER" && propsLocked) "$label · Premium" else label }
                ScrollableChoices(marketGroup(market), groups, { group ->
                    if(group=="PLAYER" && propsLocked) showLockedProps=true
                    else markets.firstOrNull { marketGroup(it)==group }?.let { market=it }
                }, enabled=!state.busy, tagPrefix="tracking-type")
                LocalizedText("Mercado", fontWeight=FontWeight.SemiBold)
                ScrollableChoices(market, markets.filter { marketGroup(it) == marketGroup(market) }
                    .map { it to marketLabel(it,event.event.sport) }, { market=it },
                    enabled=!state.busy, tagPrefix="tracking-market")
                val sides = listOf("HOME" to event.event.home.name, "AWAY" to event.event.away.name)
                if(event.event.sport=="NHL")Choice("Alcance",scope,hockeyScopes(market).map { it to hockeyScopeLabel(it) }){scope=it}
                val selections = when {
                    market=="RESULT_3WAY" || event.event.sport == "SOCCER" && market == "MONEYLINE" -> sides + ("DRAW" to "Empate")
                    market=="DOUBLE_CHANCE" -> listOf("HOME_DRAW" to "${event.event.home.name} o empate","AWAY_DRAW" to "${event.event.away.name} o empate","HOME_AWAY" to "Cualquiera gana · sin empate")
                    market in listOf("BOTH_TEAMS_SCORE","OVERTIME") -> listOf("YES" to "Sí","NO" to "No")
                    market=="CORRECT_SCORE" -> emptyList()
                    market in listOf("MONEYLINE","SPREAD") -> sides
                    else -> listOf("OVER" to "Más de (Over)","UNDER" to "Menos de (Under)")
                }
                if(selections.isNotEmpty()) {
                    LocalizedText("Selección", fontWeight=FontWeight.SemiBold)
                    ScrollableChoices(selection,selections,{selection=it},enabled=!state.busy,tagPrefix="tracking-selection")
                }
                if(market=="CORRECT_SCORE") {
                    val goals=(0..20).map { it.toString() to it.toString() }
                    Choice("Goles · ${event.event.home.name}",scoreHome,goals){scoreHome=it}
                    Choice("Goles · ${event.event.away.name}",scoreAway,goals){scoreAway=it}
                }
                if (market == "TEAM_TOTAL") Choice("Equipo", team, sides) { team = it }
                if (isPlayer) {
                    PlayerChoice(playerId, players) { if(playerId!=it){playerId=it;line=""} }
                    if (players.isEmpty()) LocalizedText(if (state.busy) "Cargando jugadores…" else if(market=="PLAYER_SAVES") "Aún no hay porteros con datos de atajadas disponibles." else "Aún no hay jugadores con datos para esta estadística.", color = Muted)
                }
                if(needsLine) {
                    key(event.id,market) {LineWheel(lineRule,line,{line=it},enabled=!state.busy)}
                    lineError?.let {LocalizedText(it,color=Red,fontSize=12.sp)}
                }
                val draft = CreateBet(event.id, market, selection, if (needsLine) parsedLine else null,
                    if (market == "TEAM_TOTAL") team else null, if (isPlayer) playerId else null, parsedStake, parsedOdds, bookmaker.takeIf { it.isNotBlank() },scope,if(market=="CORRECT_SCORE")"$scoreHome-$scoreAway" else null)
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LocalizedText("Tu selección", color = Muted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val selectedPlayer = players.find { it.id == playerId }
                            if (isPlayer && selectedPlayer != null) PlayerAvatar(selectedPlayer.imageUrl)
                            LocalizedText(draft.title(event.event, selectedPlayer?.name), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                        if(market!="MONEYLINE"&&market!="SPREAD"&&lineError==null&&line.isNotBlank())propCondition(line,selection)?.let {LocalizedText(it,color=Muted,fontSize=12.sp)}
                    }
                }
                if(builder){
                    OutlinedButton(onClick={
                        val leg=TrackingBuilderSelection(market,selection,if(needsLine)parsedLine else null,if(market=="TEAM_TOTAL")team else null)
                        if(leg !in builderLegs){updateBuilder(builderLegs+leg);line=""}
                    },enabled=valid&&!state.busy&&builderLegs.size<8&&TrackingBuilderSelection(market,selection,if(needsLine)parsedLine else null,if(market=="TEAM_TOTAL")team else null) !in builderLegs,modifier=Modifier.fillMaxWidth()){
                        LocalizedText("Añadir esta selección al boleto")
                    }
                    LocalizedText("Añade entre 2 y 8 selecciones. Usa el momio total de tu casino; las cuotas de mercados del mismo partido no se multiplican.",color=Muted,fontSize=12.sp)
                }
                TextButton(onClick = { showDetails = !showDetails }) { LocalizedText(if (showDetails) "Ocultar datos opcionales" else "Añadir monto, momio o casino") }
                if (showDetails) {
                    OutlinedTextField(stake, { stake = it }, label = { LocalizedText("Monto · opcional") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(odds, { odds = it }, label = { LocalizedText(if(builder)"Momio decimal total del boleto" else "Momio decimal · opcional (1.72)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bookmaker, { if (it.length <= 80) bookmaker = it }, label = { LocalizedText("Casino · opcional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if(addToBank)TrackingBankDestination(state.trackingBanks,selectedBankId,state.trackingBanksLoading,state.trackingBanksError,!state.busy,
                    onSelected={selectedBankId=it},onReload=model::loadTrackingBanks)
                val first=builderLegs.firstOrNull()
                val ticket=if(builder&&first!=null)draft.copy(market=first.market,selection=first.selection,line=first.line,team=first.team,playerId=null,scope=first.scope,targetScore=first.targetScore,builderSelections=builderLegs)else draft
                val ticketValid=if(builder)builderLegs.size in 2..8&&(stake.isBlank()||parsedStake!=null&&parsedStake>0)&&(odds.isBlank()||parsedOdds!=null&&parsedOdds>1)&&subscription?.bankAccess?.advanced==true else valid
                val financialDraft=if(addToBank&&destination!=null)ticket.copy(bankId=destination.id,bankVersion=destination.version)else ticket
                val operationId=rememberSaveable(financialDraft.copy(bankVersion=null)){java.util.UUID.randomUUID().toString()}
                Button(onClick = { model.create(if(addToBank)financialDraft.copy(operationId=operationId)else ticket, onSaved) },
                    enabled = ticketValid && !state.busy && (!addToBank||destination!=null&&!state.trackingBanksLoading&&state.trackingBanksError==null), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    LocalizedText(if(state.busy)"Guardando…" else if(builder&&addToBank)"Guardar creador y apuesta" else if(builder)"Guardar creador" else if(addToBank)"Guardar seguimiento y apuesta" else "Guardar seguimiento") }

                LocalizedText(when(event.event.sport) {
                    "NHL" -> "${hockeyScopeLabel(scope)}. El partido completo usa el marcador oficial, incluido el gol decisivo del shootout. Los períodos y los 60 minutos excluyen prórroga y shootout. Revisa las reglas de tu apuesta."
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
    market == "TOTAL" || market == "TEAM_TOTAL" || market.startsWith("TOTAL_") -> "TOTAL"
    else -> "RESULT"
}
internal fun kickoff(value: String,localValue: String? = null) = runCatching {
    val format=DateTimeFormatter.ofPattern("dd MMM · HH:mm")
    if(localValue!=null) java.time.OffsetDateTime.parse(localValue).format(format)
    else Instant.parse(value).atZone(ZoneId.systemDefault()).format(format)
}.getOrDefault(value)
internal fun updatedTime(value: String,localValue: String? = null) = runCatching {
    val format=DateTimeFormatter.ofPattern("dd MMM · HH:mm")
    if(localValue!=null) java.time.OffsetDateTime.parse(localValue).format(format)
    else Instant.parse(value).atZone(ZoneId.systemDefault()).format(format)
}.getOrDefault("—")
